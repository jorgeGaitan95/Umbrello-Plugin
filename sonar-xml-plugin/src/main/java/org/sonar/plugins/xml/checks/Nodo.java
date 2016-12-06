/*
 * SonarQube XML Plugin
 * Copyright (C) 2010 SonarSource
 * sonarqube@googlegroups.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sonar.plugins.xml.checks;

import java.util.ArrayList;

public class Nodo {

	String nombre;
	String id;
	String tipoNodo;
	Nodo padre;
	String tipo;
	String retorno;
	ArrayList<Nodo> atributos = new ArrayList<Nodo>();
	ArrayList<Nodo> operaciones = new ArrayList<Nodo>();
	ArrayList<Nodo> parametros = new ArrayList<Nodo>();
	
	public Nodo(String newNombre, String newId, String newTipoNodo) {
		nombre = newNombre;
		id = newId;
		tipoNodo = newTipoNodo;
	}
	
	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public ArrayList<Nodo> getAtributos() {
		return atributos;
	}

	public void setAtributos(ArrayList<Nodo> atributos) {
		this.atributos = atributos;
	}

	public ArrayList<Nodo> getOperaciones() {
		return operaciones;
	}

	public void setOperaciones(ArrayList<Nodo> operaciones) {
		this.operaciones = operaciones;
	}

	public ArrayList<Nodo> getParametros() {
		return parametros;
	}

	public void setParametros(ArrayList<Nodo> parametros) {
		this.parametros = parametros;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTipoNodo() {
		return tipoNodo;
	}

	public void setTipoNodo(String tipoNodo) {
		this.tipoNodo = tipoNodo;
	}

	public Nodo getPadre() {
		return padre;
	}

	public void setPadre(Nodo padre) {
		this.padre = padre;
	}

	public String getRetorno() {
		return retorno;
	}

	public void setRetorno(String retorno) {
		this.retorno = retorno;
	}
}
