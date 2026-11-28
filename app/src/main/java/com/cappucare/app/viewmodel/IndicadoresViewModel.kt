package com.cappucare.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cappucare.app.data.repository.IndicadoresRepository
import com.cappucare.app.model.Indicador
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/** Los tres estados posibles de la petición a la API externa (la tarjeta usa un "when"). */
sealed interface IndicadoresUiState {
    data object Cargando : IndicadoresUiState
    data class Exito(val indicadores: List<Indicador>) : IndicadoresUiState
    data class Error(val mensaje: String) : IndicadoresUiState
}

/**
 * ViewModel de la API externa (mismo patrón de la Guía 14). Está separado de CappucareViewModel:
 * si la API falla o no hay internet, el inventario (Room) sigue funcionando igual.
 */
class IndicadoresViewModel(
    private val repository: IndicadoresRepository = IndicadoresRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<IndicadoresUiState>(IndicadoresUiState.Cargando)
    val uiState: StateFlow<IndicadoresUiState> = _uiState.asStateFlow()

    // Se llama automáticamente al crear el ViewModel.
    init {
        cargarIndicadores()
    }

    /** GET /api en segundo plano. Se puede volver a llamar (botón Reintentar / Actualizar). */
    fun cargarIndicadores() {
        viewModelScope.launch {
            _uiState.value = IndicadoresUiState.Cargando
            _uiState.value = try {
                val indicadores = repository.obtenerIndicadores()
                if (indicadores.isEmpty()) {
                    IndicadoresUiState.Error(MENSAJE_SIN_DATOS)
                } else {
                    IndicadoresUiState.Exito(indicadores)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                IndicadoresUiState.Error(MENSAJE_SIN_CONEXION)
            } catch (e: HttpException) {
                IndicadoresUiState.Error("El servicio de indicadores respondió con error ${e.code()}.")
            } catch (e: Exception) {
                IndicadoresUiState.Error(MENSAJE_RESPUESTA_INVALIDA)
            }
        }
    }

    companion object {
        const val MENSAJE_SIN_CONEXION = "Sin conexión a internet. Revisa tu red y vuelve a intentar."
        const val MENSAJE_SIN_DATOS = "La API no entregó indicadores para hoy."
        const val MENSAJE_RESPUESTA_INVALIDA = "No se pudieron leer los indicadores."
    }
}
