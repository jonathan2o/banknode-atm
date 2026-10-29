package com.banknode.atm.service;

import com.banknode.atm.model.Cliente;
import java.math.BigDecimal;
import java.util.List;

/**
 * Contrato de servicio para operaciones de clientes, autenticacion y transferencias.
 */
public interface ClienteService {

    Cliente autenticar(String documento, String pin);

    Cliente consultarPorDocumento(String documento);

    BigDecimal consultarSaldo(String documento);

    void retirar(String documento, BigDecimal monto);

    void transferir(String docOrigen, String docDestino, BigDecimal monto);

    Cliente guardar(Cliente cliente);

    List<Cliente> listarTodos();
}
