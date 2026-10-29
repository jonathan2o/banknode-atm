package com.banknode.atm.model;

import com.banknode.atm.exception.AtmException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa a un cliente del banco con su cuenta y metodos de operacion.
 */
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String documento;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(nullable = false, length = 4)
    private String pin;

    @Column(nullable = false)
    private int intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    public Cliente() {
    }

    public Cliente(String documento, String nombre, BigDecimal saldo, String pin) {
        this.documento = documento;
        this.nombre = nombre;
        this.saldo = saldo != null ? saldo : BigDecimal.ZERO;
        this.pin = pin;
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }

    /**
     * Consulta el saldo actual de la cuenta del cliente.
     * @return BigDecimal con el saldo disponible
     */
    public BigDecimal consultarSaldo() {
        return this.saldo;
    }

    /**
     * Consigna un monto a la cuenta del cliente.
     * @param monto Cantidad a consignar (debe ser mayor a cero)
     */
    public void consignar(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AtmException("El monto a consignar debe ser mayor a cero.");
        }
        this.saldo = this.saldo.add(monto);
    }

    /**
     * Retira un monto de la cuenta del cliente validando fondos suficientes.
     * @param monto Cantidad a retirar
     */
    public void retirar(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AtmException("El monto a retirar debe ser mayor a cero.");
        }
        if (this.saldo.compareTo(monto) < 0) {
            throw new AtmException(String.format("Saldo insuficiente. Saldo disponible: $%s. Monto solicitado: $%s",
                    this.saldo.toPlainString(), monto.toPlainString()));
        }
        this.saldo = this.saldo.subtract(monto);
    }

    /**
     * Verifica si la cuenta se encuentra actualmente bloqueada por seguridad.
     */
    public boolean isBloqueado() {
        return this.bloqueadoHasta != null && this.bloqueadoHasta.isAfter(LocalDateTime.now());
    }

    /**
     * Incrementa el contador de intentos fallidos.
     */
    public void incrementarIntentos() {
        this.intentosFallidos++;
    }

    /**
     * Restablece el contador de intentos fallidos y anula el bloqueo.
     */
    public void resetearIntentos() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(int intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }

    @Override
    public String toString() {
        return "Cliente{" +
                "id=" + id +
                ", documento='" + documento + '\'' +
                ", nombre='" + nombre + '\'' +
                ", saldo=" + saldo +
                ", intentosFallidos=" + intentosFallidos +
                ", bloqueadoHasta=" + bloqueadoHasta +
                '}';
    }
}
