package com.cappucare.app.ui.screen.productos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cappucare.app.model.CategoriaInsumo
import com.cappucare.app.navigation.Rutas
import com.cappucare.app.ui.components.BarraSuperior
import com.cappucare.app.ui.components.BotonEscanearCodigo
import com.cappucare.app.ui.components.SelectorDesplegable
import com.cappucare.app.viewmodel.CappucareViewModel

/**
 * RF-02 / RF-08 · Catálogo con búsqueda por nombre o código (se filtra mientras se escribe) y
 * filtro por categoría. Tocar un producto abre su detalle.
 *
 * - RF-09: los productos bajo su nivel mínimo se marcan en color de alerta con un ícono.
 * - RNF-06: el ícono de cámara del buscador lee un código QR o de barras y abre ese producto.
 * - RN-08: el botón "+" (nuevo producto) solo lo ve el Personal encargado.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductosScreen(navController: NavHostController, viewModel: CappucareViewModel) {
    // Se observa la lista para que la pantalla se redibuje cuando cambie.
    val todos by viewModel.productos.collectAsState()
    var texto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf<CategoriaInsumo?>(null) }
    val resultado = remember(todos, texto, categoria) { viewModel.buscarProductos(texto, categoria) }

    val usuario by viewModel.usuarioActivo.collectAsState()
    val puedeEditar = usuario?.perfil?.puedeEditarCatalogo == true   // RN-08

    Scaffold(
        topBar = { BarraSuperior("Productos e insumos", navController) },
        floatingActionButton = {
            if (puedeEditar) {
                FloatingActionButton(onClick = {
                    viewModel.seleccionarProducto(null)
                    navController.navigate(Rutas.PRODUCTO_FORM)
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuevo producto")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Buscar por nombre o código") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    // RNF-06: escanear el código con la cámara.
                    BotonEscanearCodigo(
                        onCodigoLeido = { codigo ->
                            val encontrado = viewModel.productoPorCodigo(codigo)
                            if (encontrado != null) {
                                viewModel.seleccionarProducto(encontrado.id)
                                navController.navigate(Rutas.PRODUCTO_DETALLE)
                            } else {
                                texto = codigo
                                viewModel.avisar("No hay un producto con el código $codigo")
                            }
                        },
                        onPermisoDenegado = {
                            viewModel.avisar("Sin permiso de cámara: puedes buscar escribiendo el código")
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            SelectorDesplegable<CategoriaInsumo?>(
                etiqueta = "Categoría",
                opciones = listOf<CategoriaInsumo?>(null) + CategoriaInsumo.entries,
                seleccion = categoria,
                textoDe = { it?.etiqueta ?: "Todas las categorías" },
                onSeleccion = { categoria = it },
                textoVacio = "Todas las categorías",
                modifier = Modifier.padding(top = 8.dp)
            )

            if (resultado.isEmpty()) {
                Text(
                    "No hay productos para mostrar.",
                    modifier = Modifier.padding(top = 24.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(resultado, key = { it.id }) { producto ->
                    // RF-09 / RN-07: misma regla que Alertas de reposición (la decide el ViewModel).
                    val bajoMinimo = viewModel.estaBajoMinimo(producto)
                    Card(
                        modifier = Modifier
                            .animateItem()   // animación al filtrar (entran/salen/se reordenan)
                            .fillMaxWidth()
                            .clickable {
                                viewModel.seleccionarProducto(producto.id)
                                navController.navigate(Rutas.PRODUCTO_DETALLE)
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(producto.nombre, style = MaterialTheme.typography.titleMedium)
                            Text("${producto.codigo} · ${producto.categoria.etiqueta}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Disponible: ${producto.cantidadDisponible} ${producto.unidadMedida.etiqueta.lowercase()} · Mínimo: ${producto.nivelMinimo}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (bajoMinimo) MaterialTheme.colorScheme.error else Color.Unspecified
                            )
                            if (bajoMinimo) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Bajo el mínimo · faltan ${viewModel.faltaParaMinimo(producto)}",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            Text("Estado: ${producto.estado.etiqueta}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
