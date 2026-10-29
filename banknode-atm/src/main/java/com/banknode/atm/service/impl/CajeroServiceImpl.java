package com.banknode.atm.service.impl;

import com.banknode.atm.exception.AtmException;
import com.banknode.atm.model.Cajero;
import com.banknode.atm.repository.CajeroRepository;
import com.banknode.atm.service.CajeroService;
import com.banknode.atm.service.ClienteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementacion del servicio de Cajero que coordina las existencias de billetes y los retiros.
 */
@Service
public class CajeroServiceImpl implements CajeroService {

    private final CajeroRepository cajeroRepository;
    private final ClienteService clienteService;

    public CajeroServiceImpl(CajeroRepository cajeroRepository, ClienteService clienteService) {
        this.cajeroRepository = cajeroRepository;
        this.clienteService = clienteService;
    }

    @Override
    @Transactional
    public Cajero obtenerCajero() {
        List<Cajero> cajeros = cajeroRepository.findAll();
        if (cajeros.isEmpty()) {
            Cajero nuevo = new Cajero(50, 50, 50, 50); // 50 billetes de cada denominacion por defecto
            return cajeroRepository.save(nuevo);
        }
        return cajeros.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal consultarTotalDisponible() {
        return obtenerCajero().calcularTotalDisponible();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> consultarInventarioBilletes() {
        Cajero cajero = obtenerCajero();
        Map<String, Integer> inventario = new LinkedHashMap<>();
        inventario.put("$10.000", cajero.getBilletes10k());
        inventario.put("$20.000", cajero.getBilletes20k());
        inventario.put("$50.000", cajero.getBilletes50k());
        inventario.put("$100.000", cajero.getBilletes100k());
        return inventario;
    }

    @Override
    @Transactional
    public String procesarRetiro(String documentoCliente, BigDecimal monto) {
        Cajero cajero = obtenerCajero();

        // 1. Validar que el cajero tenga billetes y combinación para dispensar
        cajero.verificarDisponibilidad(monto);

        // 2. Validar y descontar saldo de la cuenta del cliente (lanza AtmException si saldo insuficiente)
        clienteService.retirar(documentoCliente, monto);

        // 3. Descontar los billetes del cajero físico
        String desglose = cajero.despacharBilletes(monto);
        cajeroRepository.save(cajero);

        return desglose;
    }

    @Override
    @Transactional
    public void inicializarCajeroSiNoExiste(int b10k, int b20k, int b50k, int b100k) {
        if (cajeroRepository.count() == 0) {
            Cajero cajero = new Cajero(b10k, b20k, b50k, b100k);
            cajeroRepository.save(cajero);
        }
    }
}
