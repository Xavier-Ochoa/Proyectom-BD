-- ========================================
-- SCRIPT COMPLETO PARA PGADMIN4 - SUPERMERCADO
-- ========================================

-- ========================================
-- PROCEDIMIENTOS ALMACENADOS
-- ========================================

-- 1. Inserción con validación cruzada: Agregar nuevo producto
-- Verifica si existe la categoría y actualiza el inventario
CREATE OR REPLACE PROCEDURE agregar_producto(
    p_nombre VARCHAR,
    p_descripcion TEXT,
    p_precio DECIMAL(10,2),
    p_stock INT,
    p_id_categoria INT
)
LANGUAGE plpgsql
AS $$
DECLARE
    categoria_existente INT;
BEGIN
    SELECT COUNT(*) INTO categoria_existente FROM categorias WHERE id_categoria = p_id_categoria;
    IF categoria_existente = 0 THEN
        RAISE EXCEPTION 'La categoría no existe';
    END IF;

    INSERT INTO productos (nombre, descripcion, precio, stock, id_categoria)
    VALUES (p_nombre, p_descripcion, p_precio, p_stock, p_id_categoria);

    INSERT INTO inventario (id_producto, stock_actual, stock_minimo)
    VALUES (currval('productos_id_producto_seq'), p_stock, 10);
END;
$$;

CALL agregar_producto(
    'Camiseta',
    'Camiseta de algodón talla M',
    19.99,
    50,
    2
);


-- 2. Actualización masiva: Ajustar precios por categoría
CREATE OR REPLACE PROCEDURE actualizar_precios_categoria(
    p_id_categoria INT,
    p_porcentaje DECIMAL
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE productos
    SET precio = precio + (precio * p_porcentaje / 100)
    WHERE id_categoria = p_id_categoria;
END;
$$;


CALL actualizar_precios_categoria(3, 2);
-- Esto hará que todos los productos con id_categoria = 3 tengan su precio aumentado un 2%.

-- 3. Eliminación segura: Eliminar cliente segun su id si no tiene ventas y si tine ventas no le hace nada
CREATE OR REPLACE PROCEDURE eliminar_cliente_seguro(p_id_cliente INT)
LANGUAGE plpgsql
AS $$
DECLARE
    v_existe INT;
BEGIN
    SELECT COUNT(*) INTO v_existe FROM ventas WHERE id_cliente = p_id_cliente;
    IF v_existe > 0 THEN
        RAISE NOTICE 'No se puede eliminar el cliente, tiene ventas registradas';
    ELSE
        DELETE FROM clientes WHERE id_cliente = p_id_cliente;
		RAISE NOTICE 'Cliente eliminado correctamente';
    END IF;
END;
$$;

CALL eliminar_cliente_seguro(503);

-- 4. Generar reporte de ventas por periodo y nos da el # de ventas y su total de vendido en dolares
CREATE OR REPLACE PROCEDURE reporte_ventas_periodo(
    p_fecha_inicio DATE,
    p_fecha_fin DATE
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_total_ventas INT;
    v_total_monto NUMERIC(10,2);
BEGIN
    SELECT COUNT(*), COALESCE(SUM(total), 0)
    INTO v_total_ventas, v_total_monto
    FROM ventas
    WHERE fecha_venta BETWEEN p_fecha_inicio AND p_fecha_fin;

    RAISE NOTICE 'Ventas entre % y %: % ventas, total $%', p_fecha_inicio, p_fecha_fin, v_total_ventas, v_total_monto;
END;
$$;


CALL reporte_ventas_periodo('2023-03-09', '2023-06-30');


-- 5. Facturación automática con promociones, transacciones y control de stock
-- Este procedimiento realiza una venta completa para un cliente, registrada por un 
-- empleado, con múltiples productos y sus cantidades. Valida el stock, calcula descuentos
-- si hay promociones, actualiza el stock del inventario, guarda el detalle y calcula el total.

CREATE OR REPLACE PROCEDURE realizar_venta(
    p_id_cliente INT,
    p_id_empleado INT,
    p_productos INT[],
    p_cantidades INT[]
)
LANGUAGE plpgsql
AS $$
DECLARE
    i INT := 1;
    v_total DECIMAL(10,2) := 0;
    v_precio DECIMAL(10,2);
    v_descuento DECIMAL(5,2);
    v_stock_actual INT;
    v_id_venta INT;
BEGIN
    -- Validar longitud de arrays
    IF array_length(p_productos, 1) IS DISTINCT FROM array_length(p_cantidades, 1) THEN
        RAISE EXCEPTION 'Arrays de productos y cantidades no coinciden';
    END IF;

    -- Insertar venta principal
    INSERT INTO ventas (id_cliente, id_empleado, total)
    VALUES (p_id_cliente, p_id_empleado, 0)
    RETURNING id_venta INTO v_id_venta;

    -- Recorrer productos y registrar detalle
    FOR i IN 1 .. array_length(p_productos, 1) LOOP
        -- Verificar stock
        SELECT stock_actual INTO v_stock_actual
        FROM inventario
        WHERE id_producto = p_productos[i];

        IF v_stock_actual < p_cantidades[i] THEN
            RAISE EXCEPTION 'Stock insuficiente para producto %', p_productos[i];
        END IF;

        -- Obtener precio base
        SELECT precio INTO v_precio
        FROM productos
        WHERE id_producto = p_productos[i];

        -- Calcular descuento activo (si hay)
        SELECT COALESCE(MAX(descuento_porcentaje), 0)
        INTO v_descuento
        FROM promociones p
        JOIN producto_promocion pp ON p.id_promocion = pp.id_promocion
        WHERE pp.id_producto = p_productos[i]
          AND CURRENT_DATE BETWEEN p.fecha_inicio AND p.fecha_fin;

        -- Aplicar descuento al precio
        v_precio := v_precio - (v_precio * v_descuento / 100);

        -- Acumular total
        v_total := v_total + (v_precio * p_cantidades[i]);

        -- Insertar detalle de venta
        INSERT INTO detalle_ventas(id_venta, id_producto, cantidad, precio_unitario)
        VALUES (v_id_venta, p_productos[i], p_cantidades[i], v_precio);

        -- Actualizar inventario
        UPDATE inventario
        SET stock_actual = stock_actual - p_cantidades[i]
        WHERE id_producto = p_productos[i];
    END LOOP;

    -- Actualizar total de la venta
    UPDATE ventas
    SET total = v_total
    WHERE id_venta = v_id_venta;

    -- Mensaje opcional
    RAISE NOTICE 'Venta registrada correctamente. ID venta: % Total: %', v_id_venta, v_total;

EXCEPTION WHEN OTHERS THEN
    RAISE EXCEPTION 'Error durante la venta: %', SQLERRM;
END;
$$;


CALL realizar_venta(1, 2, ARRAY[2, 3], ARRAY[1, 1]);

-- ========================================
-- FUNCIONES DEFINIDAS POR EL USUARIO
-- ========================================

-- 1. Calcular si el inventario está bajo el mínimo
CREATE OR REPLACE FUNCTION producto_stock_bajo_mensaje(p_id_producto INT)
RETURNS TABLE(stock_bajo BOOLEAN, mensaje TEXT) AS $$
DECLARE
    v_actual INT;
    v_minimo INT;
BEGIN
    SELECT stock_actual, stock_minimo 
    INTO v_actual, v_minimo 
    FROM inventario 
    WHERE id_producto = p_id_producto;

    IF v_actual < v_minimo THEN
        RETURN QUERY 
        SELECT TRUE, 
               'Stock bajo: ' || v_actual || ' unidades, mínimo requerido ' || v_minimo;
    ELSE
        RETURN QUERY 
        SELECT FALSE, 
               'Stock suficiente: ' || v_actual || ' unidades, mínimo requerido ' || v_minimo;
    END IF;
END;
$$ LANGUAGE plpgsql;




SELECT * FROM producto_stock_bajo_mensaje(5);


-- 2. Calcular total de compras por proveedor
CREATE OR REPLACE FUNCTION total_compras_proveedor(p_id INT)
RETURNS DECIMAL(10,2) AS $$
DECLARE
    v_total DECIMAL(10,2);
BEGIN
    SELECT COALESCE(SUM(total), 0) INTO v_total FROM compras WHERE id_proveedor = p_id;
    RETURN v_total;
END;
$$ LANGUAGE plpgsql;



SELECT total_compras_proveedor(3);


-- 3. Verificar si cliente es frecuente si tiene al menos 5 ventas registradas
CREATE OR REPLACE FUNCTION es_cliente_frecuente(p_id_cliente INT)
RETURNS TEXT AS $$
DECLARE
    v_ventas INT;
BEGIN
    SELECT COUNT(*) INTO v_ventas FROM ventas WHERE id_cliente = p_id_cliente;

    IF v_ventas >= 5 THEN
        RETURN 'Cliente frecuente: tiene ' || v_ventas || ' ventas.';
    ELSE
        RETURN 'Cliente no frecuente: tiene solo ' || v_ventas || ' ventas.';
    END IF;
END;
$$ LANGUAGE plpgsql;


SELECT es_cliente_frecuente(123);


-- ======================================
-- TRIGGERS PARA FUNCIONALIDADES CLAVE
-- ======================================

-- 1. Trigger para auditoría general en INSERT, UPDATE, DELETE de la tabla productos
CREATE OR REPLACE FUNCTION auditoria_prod_fn() RETURNS trigger AS $$
BEGIN
    INSERT INTO bitacora(nombre_tabla, accion, id_registro, usuario)
    VALUES (TG_TABLE_NAME, TG_OP, NEW.id_producto, CURRENT_USER);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER auditoria_prod_trg
AFTER INSERT OR UPDATE OR DELETE ON productos
FOR EACH ROW EXECUTE FUNCTION auditoria_prod_fn();





-- 2. Validación de fechas vencidas en promociones

CREATE OR REPLACE FUNCTION fn_validar_fechas_promocion()
RETURNS TRIGGER AS $$
BEGIN
    -- No permitir que la promoción comience en el pasado
    IF NEW.fecha_inicio < CURRENT_DATE THEN
        RAISE EXCEPTION 'La fecha de inicio no puede ser anterior a hoy.';
    END IF;

    -- No permitir que la fecha de fin sea anterior a la fecha de inicio
    IF NEW.fecha_fin < NEW.fecha_inicio THEN
        RAISE EXCEPTION 'La fecha de fin no puede ser anterior a la fecha de inicio.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger que ejecuta la validación antes de insertar o actualizar
CREATE TRIGGER trg_validar_fechas_promocion
BEFORE INSERT OR UPDATE ON promociones
FOR EACH ROW
EXECUTE FUNCTION fn_validar_fechas_promocion();

 
-- 3. Trigger para antes de insertar una venta en detalle_ventas:
--  Consulta el stock del producto en inventario.
   -- Lanza un error si:
     --   No hay inventario registrado para ese producto.
       -- El stock es 0.
        -- La cantidad pedida es mayor que el stock.
CREATE OR REPLACE FUNCTION fn_validar_stock_venta()
RETURNS trigger AS $$
DECLARE
    v_stock_actual INT;
BEGIN
    -- Obtener el stock actual del producto
    SELECT stock_actual INTO v_stock_actual
    FROM inventario
    WHERE id_producto = NEW.id_producto;

    -- Si no hay inventario registrado, error
    IF v_stock_actual IS NULL THEN
        RAISE EXCEPTION 'No hay inventario registrado para el producto %.', NEW.id_producto;
    END IF;

    -- Si el stock es cero o menor, no se permite vender
    IF v_stock_actual = 0 THEN
        RAISE EXCEPTION 'El producto % no tiene stock disponible.', NEW.id_producto;
    END IF;

    -- Si la cantidad pedida excede el stock, no se permite vender
    IF NEW.cantidad > v_stock_actual THEN
        RAISE EXCEPTION 'Cantidad solicitada (% unidades) excede el stock disponible (% unidades) para el producto %.',
            NEW.cantidad, v_stock_actual, NEW.id_producto;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_validar_stock_venta
BEFORE INSERT ON detalle_ventas
FOR EACH ROW
EXECUTE FUNCTION fn_validar_stock_venta();



-- =============================
-- VISTAS POR ROL FUNCIONAL
-- =============================

-- Vista para el cajero: productos y precios
CREATE OR REPLACE VIEW vista_cajero AS
SELECT p.id_producto, p.nombre, p.precio, i.stock_actual
FROM productos p
JOIN inventario i ON p.id_producto = i.id_producto;


SELECT * FROM vista_cajero;


-- Vista para el administrador: ventas detalladas con promociones
CREATE OR REPLACE VIEW vista_administrador AS
SELECT v.id_venta, v.fecha_venta, c.nombre AS cliente, e.nombre AS empleado, p.nombre AS producto, dv.cantidad, dv.precio_unitario,
       pr.nombre_promocion, pr.descuento_porcentaje
FROM ventas v
JOIN clientes c ON v.id_cliente = c.id_cliente
JOIN empleados e ON v.id_empleado = e.id_empleado
JOIN detalle_ventas dv ON v.id_venta = dv.id_venta
JOIN productos p ON dv.id_producto = p.id_producto
LEFT JOIN producto_promocion pp ON p.id_producto = pp.id_producto
LEFT JOIN promociones pr ON pp.id_promocion = pr.id_promocion AND v.fecha_venta BETWEEN pr.fecha_inicio AND pr.fecha_fin;

SELECT * FROM vista_administrador;


-- Vista para bodega: control de inventario
CREATE OR REPLACE VIEW vista_bodega AS
SELECT p.id_producto, p.nombre, i.stock_actual, i.stock_minimo, c.nombre_categoria, p.descripcion
FROM inventario i
JOIN productos p ON i.id_producto = p.id_producto
JOIN categorias c ON p.id_categoria = c.id_categoria;


SELECT * FROM vista_bodega;

-- ==========================
-- ÍNDICES PARA OPTIMIZACIÓN
-- ==========================

-- Tabla: productos
CREATE INDEX idx_productos_nombre ON productos(nombre);
CREATE INDEX idx_productos_id_categoria ON productos(id_categoria);

-- Tabla: clientes
CREATE INDEX idx_clientes_cedula ON clientes(cedula);
CREATE INDEX idx_clientes_email ON clientes(email);

-- Tabla: empleados
CREATE INDEX idx_empleados_nombre ON empleados(nombre);

-- Tabla: proveedores
CREATE INDEX idx_proveedores_nombre ON proveedores(nombre);

-- Tabla: compras
CREATE INDEX idx_compras_id_proveedor ON compras(id_proveedor);
CREATE INDEX idx_compras_fecha_compra ON compras(fecha_compra);

-- Tabla: ventas
CREATE INDEX idx_ventas_id_cliente ON ventas(id_cliente);
CREATE INDEX idx_ventas_id_empleado ON ventas(id_empleado);
CREATE INDEX idx_ventas_fecha_venta ON ventas(fecha_venta);

-- Tabla: detalle_compras
CREATE INDEX idx_detalle_compras_id_compra ON detalle_compras(id_compra);
CREATE INDEX idx_detalle_compras_id_producto ON detalle_compras(id_producto);

-- Tabla: detalle_ventas
CREATE INDEX idx_detalle_ventas_id_venta ON detalle_ventas(id_venta);
CREATE INDEX idx_detalle_ventas_id_producto ON detalle_ventas(id_producto);

-- Tabla: inventario
CREATE INDEX idx_inventario_stock_actual ON inventario(stock_actual);

-- Tabla: bitacora
CREATE INDEX idx_bitacora_nombre_tabla ON bitacora(nombre_tabla);
CREATE INDEX idx_bitacora_accion ON bitacora(accion);
CREATE INDEX idx_bitacora_fecha ON bitacora(fecha);

-- Tabla: promociones
CREATE INDEX idx_promociones_fecha_inicio ON promociones(fecha_inicio);
CREATE INDEX idx_promociones_fecha_fin ON promociones(fecha_fin);

-- Tabla: producto_promocion
CREATE INDEX idx_producto_promocion_id_producto ON producto_promocion(id_producto);
CREATE INDEX idx_producto_promocion_id_promocion ON producto_promocion(id_promocion);


-- Combinaciones más consultadas en detalle de ventas y compras
CREATE INDEX idx_detalle_compras_compuesto ON detalle_compras(id_compra, id_producto);
CREATE INDEX idx_detalle_ventas_compuesto ON detalle_ventas(id_venta, id_producto);

-- Promociones activas (por fechas)
CREATE INDEX idx_promociones_activas ON promociones(fecha_inicio, fecha_fin);

-- Bitácora por tabla y fecha (útil para reportes de auditoría)
CREATE INDEX idx_bitacora_tabla_fecha ON bitacora(nombre_tabla, fecha);

-- EXPLAIN ANALYZE - Si ves que usa "Index Scan" en lugar de "Seq Scan", ¡el índice está funcionando bien!
EXPLAIN ANALYZE
SELECT *
FROM detalle_ventas
WHERE id_venta = 123 AND id_producto = 5;


=============================
-- CONSULTAS DE REPORTE ÚTILES
-- =============================

-- Productos más vendidos
CREATE OR REPLACE VIEW reporte_productos_mas_vendidos AS
SELECT p.id_producto, p.nombre, SUM(dv.cantidad) AS total_vendido
FROM detalle_ventas dv
JOIN productos p ON dv.id_producto = p.id_producto
GROUP BY p.id_producto, p.nombre
ORDER BY total_vendido DESC;

-- Inventario actual
CREATE OR REPLACE VIEW reporte_inventario_actual AS
SELECT p.nombre, i.stock_actual, i.stock_minimo
FROM productos p
JOIN inventario i ON p.id_producto = i.id_producto;

-- Promociones activas
CREATE OR REPLACE VIEW reporte_promociones_activas AS
SELECT *
FROM promociones
WHERE CURRENT_DATE BETWEEN fecha_inicio AND fecha_fin;

-- Clientes frecuentes
CREATE OR REPLACE VIEW reporte_clientes_frecuentes AS
SELECT c.id_cliente, c.nombre, COUNT(v.id_venta) AS total_compras
FROM ventas v
JOIN clientes c ON v.id_cliente = c.id_cliente
GROUP BY c.id_cliente, c.nombre
ORDER BY total_compras DESC;



-- FUNCIONES MODIFiCADAS --------------------------------------------------------------------

CREATE OR REPLACE FUNCTION realizar_venta_FN(
    p_id_cliente INT,
    p_id_empleado INT,
    p_productos INT[],
    p_cantidades INT[]
) RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
    i INT := 1;
    v_total DECIMAL(10,2) := 0;
    v_precio DECIMAL(10,2);
    v_descuento DECIMAL(5,2);
    v_stock_actual INT;
    v_id_venta INT;
BEGIN
    -- Validación básica de arrays
    IF array_length(p_productos, 1) IS DISTINCT FROM array_length(p_cantidades, 1) THEN
        RAISE EXCEPTION 'Los arrays de productos y cantidades deben tener la misma longitud.';
    END IF;

    -- Validar existencia de cliente
    IF NOT EXISTS (SELECT 1 FROM clientes WHERE id_cliente = p_id_cliente) THEN
        RAISE EXCEPTION 'Cliente con ID % no existe.', p_id_cliente;
    END IF;

    -- Validar existencia de empleado
    IF NOT EXISTS (SELECT 1 FROM empleados WHERE id_empleado = p_id_empleado) THEN
        RAISE EXCEPTION 'Empleado con ID % no existe.', p_id_empleado;
    END IF;

    -- Insertar venta
    INSERT INTO ventas (id_cliente, id_empleado, total)
    VALUES (p_id_cliente, p_id_empleado, 0)
    RETURNING id_venta INTO v_id_venta;

    -- Procesar cada producto
    FOR i IN 1 .. array_length(p_productos, 1) LOOP
        -- Validar producto
        IF NOT EXISTS (SELECT 1 FROM productos WHERE id_producto = p_productos[i]) THEN
            RAISE EXCEPTION 'Producto con ID % no existe.', p_productos[i];
        END IF;

        -- Verificar stock
        SELECT stock_actual INTO v_stock_actual
        FROM inventario
        WHERE id_producto = p_productos[i];

        IF v_stock_actual IS NULL THEN
            RAISE EXCEPTION 'Producto % no tiene registro en inventario.', p_productos[i];
        ELSIF v_stock_actual < p_cantidades[i] THEN
            RAISE EXCEPTION 'Stock insuficiente para producto %.', p_productos[i];
        END IF;

        -- Precio base
        SELECT precio INTO v_precio
        FROM productos
        WHERE id_producto = p_productos[i];

        -- Descuento activo
        SELECT COALESCE(MAX(descuento_porcentaje), 0)
        INTO v_descuento
        FROM promociones p
        JOIN producto_promocion pp ON p.id_promocion = pp.id_promocion
        WHERE pp.id_producto = p_productos[i]
          AND CURRENT_DATE BETWEEN p.fecha_inicio AND p.fecha_fin;

        -- Precio con descuento
        v_precio := v_precio - (v_precio * v_descuento / 100);

        -- Sumar al total
        v_total := v_total + (v_precio * p_cantidades[i]);

        -- Insertar detalle
        INSERT INTO detalle_ventas(id_venta, id_producto, cantidad, precio_unitario)
        VALUES (v_id_venta, p_productos[i], p_cantidades[i], v_precio);

        -- Descontar del inventario
        UPDATE inventario
        SET stock_actual = stock_actual - p_cantidades[i]
        WHERE id_producto = p_productos[i];
    END LOOP;

    -- Actualizar total de la venta
    UPDATE ventas
    SET total = v_total
    WHERE id_venta = v_id_venta;

    -- Retornar ID de la venta
    RETURN v_id_venta;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION 'Error durante el proceso de venta: %', SQLERRM;
END;
$$;



--  ingresar un PRODUCTO


CREATE OR REPLACE FUNCTION ingresar_producto_con_stock(
    p_nombre VARCHAR,
    p_descripcion TEXT,
    p_precio DECIMAL,
    p_id_categoria INT,
    p_stock_actual INT,
    p_stock_minimo INT
)
RETURNS VOID AS $$
DECLARE
    v_id_producto INT;
BEGIN
    -- Validaciones opcionales
    IF p_precio < 0 THEN
        RAISE EXCEPTION 'El precio no puede ser negativo.';
    END IF;

    IF p_stock_actual < 0 OR p_stock_minimo < 0 THEN
        RAISE EXCEPTION 'Los valores de stock no pueden ser negativos.';
    END IF;

    -- Insertar en productos
    INSERT INTO productos(nombre, descripcion, precio, id_categoria)
    VALUES (p_nombre, p_descripcion, p_precio, p_id_categoria)
    RETURNING id_producto INTO v_id_producto;

    -- Insertar en inventario
    INSERT INTO inventario(id_producto, stock_actual, stock_minimo)
    VALUES (v_id_producto, p_stock_actual, p_stock_minimo);
    
    RAISE NOTICE 'Producto ingresado con ID: %', v_id_producto;
END;
$$ LANGUAGE plpgsql;

-- PROMOCIONES --------

CREATE OR REPLACE FUNCTION insertar_promocion_con_producto_opcional(
    p_nombre_promocion VARCHAR,
    p_fecha_inicio DATE,
    p_fecha_fin DATE,
    p_descripcion TEXT DEFAULT NULL,
    p_descuento_porcentaje DECIMAL(5,2) DEFAULT NULL,
    p_id_producto INT DEFAULT NULL,
    p_id_categoria INT DEFAULT NULL
) RETURNS VOID AS $$
DECLARE
    v_id_promocion INT;
BEGIN
    -- Validaciones obligatorias
    IF p_nombre_promocion IS NULL OR LENGTH(TRIM(p_nombre_promocion)) = 0 THEN
        RAISE EXCEPTION 'El nombre_promocion es obligatorio';
    END IF;

    IF p_fecha_inicio IS NULL THEN
        RAISE EXCEPTION 'La fecha_inicio es obligatoria';
    END IF;

    IF p_fecha_fin IS NULL THEN
        RAISE EXCEPTION 'La fecha_fin es obligatoria';
    END IF;

    IF p_fecha_fin < p_fecha_inicio THEN
        RAISE EXCEPTION 'La fecha_fin debe ser mayor o igual a fecha_inicio';
    END IF;

    IF p_descuento_porcentaje IS NOT NULL THEN
        IF p_descuento_porcentaje < 0 OR p_descuento_porcentaje > 100 THEN
            RAISE EXCEPTION 'El descuento_porcentaje debe estar entre 0 y 100';
        END IF;
    END IF;

    -- Insertar la promoción
    INSERT INTO promociones(nombre_promocion, descripcion, descuento_porcentaje, fecha_inicio, fecha_fin)
    VALUES (p_nombre_promocion, p_descripcion, p_descuento_porcentaje, p_fecha_inicio, p_fecha_fin)
    RETURNING id_promocion INTO v_id_promocion;

    -- Asociar producto individual si se proporciona
    IF p_id_producto IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM productos WHERE id_producto = p_id_producto) THEN
            RAISE EXCEPTION 'El producto con id % no existe', p_id_producto;
        END IF;

        INSERT INTO producto_promocion(id_producto, id_promocion)
        VALUES (p_id_producto, v_id_promocion);
    END IF;

    -- Asociar todos los productos de la categoría si se proporciona
    IF p_id_categoria IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM categorias WHERE id_categoria = p_id_categoria) THEN
            RAISE EXCEPTION 'La categoría con id % no existe', p_id_categoria;
        END IF;

        INSERT INTO producto_promocion(id_producto, id_promocion)
        SELECT id_producto, v_id_promocion
        FROM productos
        WHERE id_categoria = p_id_categoria
        ON CONFLICT DO NOTHING;  -- Evita duplicados si ya se asoció el producto
    END IF;

END;
$$ LANGUAGE plpgsql;

SELECT insertar_promocion_con_producto_opcional(
    'Promo Combo',             -- p_nombre_promocion
    '2025-08-02',              -- p_fecha_inicio
    '2025-08-15',              -- p_fecha_fin
    'Promo para un producto y toda una categoría', -- p_descripcion
    20.00,                    -- p_descuento_porcentaje
    15,                       -- p_id_producto
    5                         -- p_id_categoria
);


-- CATEGORIAS



CREATE OR REPLACE FUNCTION insertar_categoria(
    p_nombre_categoria VARCHAR,
    p_descripcion TEXT DEFAULT NULL,
    p_es_perecible BOOLEAN DEFAULT FALSE
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO categorias(nombre_categoria, descripcion, es_perecible)
    VALUES (p_nombre_categoria, p_descripcion, p_es_perecible);
END;
$$ LANGUAGE plpgsql;


-- EMPLEADO

CREATE OR REPLACE FUNCTION insertar_empleado(
    p_nombre VARCHAR,
    p_cedula VARCHAR,
    p_cargo VARCHAR,
    p_telefono VARCHAR,
    p_fecha_contratacion DATE DEFAULT CURRENT_DATE
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO empleados(nombre, cedula, cargo, telefono, fecha_contratacion)
    VALUES (p_nombre, p_cedula, p_cargo, p_telefono, p_fecha_contratacion);
END;
$$ LANGUAGE plpgsql;


-- PROVEEDORES--------------------

CREATE OR REPLACE FUNCTION ingresar_proveedor(
    p_nombre VARCHAR,
    p_contacto VARCHAR DEFAULT NULL,
    p_telefono VARCHAR DEFAULT NULL,
    p_direccion TEXT DEFAULT NULL
) RETURNS INT AS $$
DECLARE
    nuevo_id INT;
BEGIN
    INSERT INTO proveedores (nombre, contacto, telefono, direccion)
    VALUES (p_nombre, p_contacto, p_telefono, p_direccion)
    RETURNING id_proveedor INTO nuevo_id;

    RETURN nuevo_id;
END;
$$ LANGUAGE plpgsql;





