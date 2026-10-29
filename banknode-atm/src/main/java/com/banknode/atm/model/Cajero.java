package com.banknode.atm.model;

import com.banknode.atm.exception.AtmException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Entidad JPA que representa el Cajero Automático y gestiona el inventario de billetes.
 */
@Entity
@Table(name = "cajeros")
public class Cajero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int billetes10k = 0;

    @Column(nullable = false)
    private int billetes20k = 0;

    @Column(nullable = false)
    private int billetes50k = 0;

    @Column(nullable = false)
    private int billetes100k = 0;

    public Cajero() {
    }

    public Cajero(int billetes10k, int billetes20k, int billetes50k, int billetes100k) {
        this.billetes10k = billetes10k;
        this.billetes20k = billetes20k;
        this.billetes50k = billetes50k;
        this.billetes100k = billetes100k;
    }

    /**
     * Calcula la suma total en efectivo disponible actualmente en el cajero.
     * @return BigDecimal con el total disponible
     */
    public BigDecimal calcularTotalDisponible() {
        long total = ((long) billetes10k * 10000L)
                + ((long) billetes20k * 20000L)
                + ((long) billetes50k * 50000L)
                + ((long) billetes100k * 100000L);
        return BigDecimal.valueOf(total);
    }

    /**
     * Verifica si el cajero puede dispensar el monto requerido con la combinación actual de billetes.
     * @param monto Monto solicitado a retirar
     * @return Map con la cantidad de billetes por denominación necesarios
     */
    public Map<Integer, Integer> verificarDisponibilidad(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AtmException("El monto a retirar debe ser mayor a cero.");
        }

        long valor = monto.longValue();

        if (valor % 10000L != 0) {
            throw new AtmException("El cajero solo permite retiros en múltiplos de $10.000 COP.");
        }

        if (monto.compareTo(calcularTotalDisponible()) > 0) {
            throw new AtmException(String.format(
                    "El cajero no cuenta con suficiente efectivo. Disponible: $%s COP, Solicitado: $%s COP",
                    calcularTotalDisponible().toPlainString(), monto.toPlainString()));
        }

        Map<Integer, Integer> distribucion = new HashMap<>();
        long restante = valor;

        // 1. Billetes de 100k
        int necesarios100k = (int) Math.min(restante / 100000L, this.billetes100k);
        restante -= necesarios100k * 100000L;
        distribucion.put(100000, necesarios100k);

        // 2. Billetes de 50k
        int necesarios50k = (int) Math.min(restante / 50000L, this.billetes50k);
        restante -= necesarios50k * 50000L;
        distribucion.put(50000, necesarios50k);

        // 3. Billetes de 20k
        int necesarios20k = (int) Math.min(restante / 20000L, this.billetes20k);
        restante -= necesarios20k * 20000L;
        distribucion.put(20000, necesarios20k);

        // 4. Billetes de 10k
        int necesarios10k = (int) Math.min(restante / 10000L, this.billetes10k);
        restante -= necesarios10k * 10000L;
        distribucion.put(10000, necesarios10k);

        if (restante > 0) {
            throw new AtmException("No es posible dispensar el monto exacto con las denominaciones de billetes disponibles actualmente en el cajero.");
        }

        return distribucion;
    }

    /**
     * Despacha los billetes y descuenta las existencias del cajero.
     * @param monto Monto a retirar
     * @return Mensaje descriptivo con el desglose de billetes entregados
     */
    public String despacharBilletes(BigDecimal monto) {
        Map<Integer, Integer> distribucion = verificarDisponibilidad(monto);

        int usados100k = distribucion.getOrDefault(100000, 0);
        int usados50k = distribucion.getOrDefault(50000, 0);
        int usados20k = distribucion.getOrDefault(20000, 0);
        int usados10k = distribucion.getOrDefault(10000, 0);

        this.billetes100k -= usados100k;
        this.billetes50k -= usados50k;
        this.billetes20k -= usados20k;
        this.billetes10k -= usados10k;

        StringBuilder sb = new StringBuilder("Billetes entregados: ");
        boolean primero = true;
        if (usados100k > 0) {
            sb.append(usados100k).append(" de $100.000");
            primero = false;
        }
        if (usados50k > 0) {
            if (!primero) sb.append(", ");
            sb.append(usados50k).append(" de $50.000");
            primero = false;
        }
        if (usados20k > 0) {
            if (!primero) sb.append(", ");
            sb.append(usados20k).append(" de $20.000");
            primero = false;
        }
        if (usados10k > 0) {
            if (!primero) sb.append(", ");
            sb.append(usados10k).append(" de $10.000");
        }

        return sb.toString();
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getBilletes10k() {
        return billetes10k;
    }

    public void setBilletes10k(int billetes10k) {
        this.billetes10k = billetes10k;
    }

    public int getBilletes20k() {
        return billetes20k;
    }

    public void setBilletes20k(int billetes20k) {
        this.billetes20k = billetes20k;
    }

    public int getBilletes50k() {
        return billetes50k;
    }

    public void setBilletes50k(int billetes50k) {
        this.billetes50k = billetes50k;
    }

    public int getBilletes100k() {
        return billetes100k;
    }

    public void setBilletes100k(int billetes100k) {
        this.billetes100k = billetes100k;
    }
}
