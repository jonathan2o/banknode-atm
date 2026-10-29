package com.banknode.atm.service.impl;

import com.banknode.atm.exception.AtmException;
import com.banknode.atm.model.Cliente;
import com.banknode.atm.repository.ClienteRepository;
import com.banknode.atm.service.ClienteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementacion del servicio de Clientes con politicas de bloqueo de 10 minutos y transacciones atomicas.
 */
@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    @Transactional
    public Cliente autenticar(String documento, String pin) {
        if (documento == null || documento.trim().isEmpty()) {
            throw new AtmException("El documento es obligatorio.");
        }
        if (pin == null || pin.trim().length() != 4) {
            throw new AtmException("El PIN debe ser exactamente de 4 dígitos numéricos.");
        }

        Cliente cliente = clienteRepository.findByDocumento(documento.trim())
                .orElseThrow(() -> new AtmException("Cliente con documento " + documento + " no existe en el sistema."));

        // Validar si la cuenta está actualmente bloqueada
        if (cliente.isBloqueado()) {
            long minutosRestantes = Duration.between(LocalDateTime.now(), cliente.getBloqueadoHasta()).toMinutes() + 1;
            throw new AtmException(String.format(
                    "Cuenta bloqueada por seguridad tras 3 intentos fallidos. Intente nuevamente en %d minuto(s).",
                    minutosRestantes));
        }

        // Si el periodo de bloqueo ya venció, restablecer estado
        if (cliente.getBloqueadoHasta() != null && !cliente.isBloqueado()) {
            cliente.resetearIntentos();
        }

        // Validación del PIN
        if (cliente.getPin().equals(pin.trim())) {
            cliente.resetearIntentos();
            return clienteRepository.save(cliente);
        } else {
            cliente.incrementarIntentos();
            if (cliente.getIntentosFallidos() >= 3) {
                cliente.setBloqueadoHasta(LocalDateTime.now().plusMinutes(10));
                clienteRepository.save(cliente);
                throw new AtmException("Ha superado el límite de 3 intentos fallidos. Su cuenta ha sido bloqueada durante 10 minutos.");
            } else {
                int intentosRestantes = 3 - cliente.getIntentosFallidos();
                clienteRepository.save(cliente);
                throw new AtmException(String.format("PIN incorrecto. Le resta(n) %d intento(s) antes del bloqueo temporal.", intentosRestantes));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente consultarPorDocumento(String documento) {
        return clienteRepository.findByDocumento(documento.trim())
                .orElseThrow(() -> new AtmException("Cliente con documento " + documento + " no encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal consultarSaldo(String documento) {
        Cliente cliente = consultarPorDocumento(documento);
        return cliente.consultarSaldo();
    }

    @Override
    @Transactional
    public void retirar(String documento, BigDecimal monto) {
        Cliente cliente = consultarPorDocumento(documento);
        cliente.retirar(monto);
        clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public void transferir(String docOrigen, String docDestino, BigDecimal monto) {
        if (docOrigen.trim().equalsIgnoreCase(docDestino.trim())) {
            throw new AtmException("No es posible transferir dinero a la misma cuenta de origen.");
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AtmException("El monto a transferir debe ser mayor a cero.");
        }

        Cliente origen = consultarPorDocumento(docOrigen);
        Cliente destino = clienteRepository.findByDocumento(docDestino.trim())
                .orElseThrow(() -> new AtmException("La cuenta de destino con documento " + docDestino + " no existe."));

        origen.retirar(monto);
        destino.consignar(monto);

        clienteRepository.save(origen);
        clienteRepository.save(destino);
    }

    @Override
    @Transactional
    public Cliente guardar(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }
}
