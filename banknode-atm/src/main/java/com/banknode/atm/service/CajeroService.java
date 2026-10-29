package com.banknode.atm.service;

import com.banknode.atm.model.Cajero;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Contrato de servicio para la gestion del inventario y operaciones de efectivo del Cajero.
 */
public interface CajeroService {

    Cajero obtenerCajero();

    BigDecimal consultarTotalDisponible();

    Map<String, Integer> consultarInventarioBilletes();

    String procesarRetiro(String documentoCliente, BigDecimal monto);

    void inicializarCajeroSiNoExiste(int b10k, int b20k, int b50k, int b100k);
}
