package com.hxiloj.dto;

public class ObligadoTributario {

	public ObligadoTributario() {
	}

	public ObligadoTributario(String tipoIdentificacion, String numeroIdentificacion) {
		super();
		this.tipoIdentificacion = tipoIdentificacion;
		this.numeroIdentificacion = numeroIdentificacion;
	}

	private String tipoIdentificacion;
	private String numeroIdentificacion;

	public String getTipoIdentificacion() {
		return tipoIdentificacion;
	}

	public void setTipoIdentificacion(String tipoIdentificacion) {
		this.tipoIdentificacion = tipoIdentificacion;
	}

	public String getNumeroIdentificacion() {
		return numeroIdentificacion;
	}

	public void setNumeroIdentificacion(String numeroIdentificacion) {
		this.numeroIdentificacion = numeroIdentificacion;
	}
}
