package com.cappucare.app.data.local

import com.cappucare.app.model.CategoriaInsumo
import com.cappucare.app.model.EstadoProducto
import com.cappucare.app.model.MotivoSalida
import com.cappucare.app.model.Movimiento
import com.cappucare.app.model.Perfil
import com.cappucare.app.model.Producto
import com.cappucare.app.model.TipoMovimiento
import com.cappucare.app.model.UnidadMedida
import com.cappucare.app.model.Usuario

/*
 * Conversión entre las ENTIDADES de Room (data/local) y los MODELOS que usan el ViewModel y
 * las pantallas (model/). Así la interfaz no depende de cómo se guardan los datos.
 */

fun UsuarioEntity.aModelo() = Usuario(id, nombre, usuario, claveHash, Perfil.valueOf(perfil))
fun Usuario.aEntidad() = UsuarioEntity(id, nombre, usuario.trim().lowercase(), claveHash, perfil.name)

fun ProductoEntity.aModelo() = Producto(
    id = id,
    codigo = codigo,
    nombre = nombre,
    descripcion = descripcion,
    categoria = CategoriaInsumo.valueOf(categoria),
    unidadMedida = UnidadMedida.valueOf(unidadMedida),
    cantidadDisponible = cantidadDisponible,
    nivelMinimo = nivelMinimo,
    ubicacion = ubicacion,
    estado = EstadoProducto.valueOf(estado)
)

fun Producto.aEntidad() = ProductoEntity(
    id = id,
    codigo = codigo,
    nombre = nombre,
    descripcion = descripcion,
    categoria = categoria.name,
    unidadMedida = unidadMedida.name,
    cantidadDisponible = cantidadDisponible,
    nivelMinimo = nivelMinimo,
    ubicacion = ubicacion,
    estado = estado.name
)

fun MovimientoEntity.aModelo() = Movimiento(
    id = id,
    productoId = productoId,
    fecha = fecha,
    tipo = TipoMovimiento.valueOf(tipo),
    cantidad = cantidad,
    motivo = motivo?.let { MotivoSalida.valueOf(it) },
    observacion = observacion,
    responsable = responsable,
    responsablePerfil = Perfil.valueOf(responsablePerfil)
)

fun Movimiento.aEntidad() = MovimientoEntity(
    id = id,
    productoId = productoId,
    fecha = fecha,
    tipo = tipo.name,
    cantidad = cantidad,
    motivo = motivo?.name,
    observacion = observacion,
    responsable = responsable,
    responsablePerfil = responsablePerfil.name
)
