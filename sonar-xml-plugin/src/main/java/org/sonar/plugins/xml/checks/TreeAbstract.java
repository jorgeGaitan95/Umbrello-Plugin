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
import java.util.Hashtable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;	

public class TreeAbstract {

	ArrayList<Nodo> clases = new ArrayList<Nodo>();
	Hashtable<String, Nodo> dataType= new Hashtable<String, Nodo>();
	Hashtable<String, Nodo> packages= new Hashtable<String, Nodo>();
	ArrayList<Relacion> relaciones = new ArrayList<Relacion>();
	ArrayList<String> palabrasReservadas=new ArrayList<String>();
	// lista de errores cuando la clase no tiene un nombre 
	ArrayList<Node> ErroresNombre= new ArrayList<Node>();
	// Lista de errores de atributos sin tipo
	ArrayList<Node> ErroresTipoAtributos= new ArrayList<Node>();
	//Lista de errores de tipos invalidos de los atrbutos
	ArrayList<Node> ErroresTipoValidoAtributos= new ArrayList<Node>();
	//Lista de errore de elementos con nombres invalidos 
	ArrayList<Node> ErroresNombreInvalidos= new ArrayList<Node>();
	// Lista de errores de relacion sin source o target
	ArrayList<Node> ErroresRelaciones= new ArrayList<Node>();
	// Lista de errores de elementos con palabras reservadas en los nombres
	ArrayList<Node> ErroresPalabrasReservadas= new ArrayList<Node>();
	private static TreeAbstract instance;
	private static XmlSourceCode instancexml;

	private TreeAbstract(XmlSourceCode archivoXml) {
		Document document = archivoXml.getDocument(false);
		if (document.getDocumentElement() != null) {
			instancexml=archivoXml;
			init(document);
		}
	}

	public static TreeAbstract getInstance(XmlSourceCode xml){
		if(instancexml!=xml){
			instance=null;
		}
		if(instance==null){
			instance=new TreeAbstract(xml);
		}
		return instance;
	}

	public void init(Document doc){
		doc.getDocumentElement().normalize();
		initPalabrasReservadas();
		//Lectura de Nodos
		ArmarDataTypes(doc);
		ArmarPackages(doc);
		ArmarClases(doc);

		//Lectura de Relaciones
		RelationContainment(doc);
		RelacionDependencia(doc);
		RelacionGeneralizacion(doc);
		RelationAsociation(doc);
	}

	private void ArmarClases(Document doc) {
		NodeList nList = doc.getElementsByTagName("UML:Class");

		for (int temp = 0; temp < nList.getLength(); temp++) {

			Node nNode = nList.item(temp);
			Element eElement = (Element) nNode;

			//Creaci�n del Nodo Clasee
			Node className=nNode.getAttributes().getNamedItem("name");
			String name = "";
			//Valida que la clase tenga un nombre
			if(className!=null){
				name= className.getNodeValue();
				if(name.equals("")){
					ErroresNombre.add(nNode);
				}else{
					validarNombreElementos(name, nNode);
				}
			}else{
				ErroresNombre.add(nNode);
			}

			String id = nNode.getAttributes().getNamedItem("xmi.id").getNodeValue();
			Nodo clase = new Nodo(name, id, "Clase",nNode);
			try {
				NodeList nListAux = eElement.getElementsByTagName("UML:Attribute");
				NamedNodeMap attributes;
				//Busqueda del subElemento Attribute
				for (int i = 0; i < nListAux.getLength(); i++){
					attributes=nListAux.item(i).getAttributes();
					//Creaci�n del nodo Atributo
					Node attNombre = attributes.getNamedItem("name");
					String nombreAtributo="";
					if(attNombre!=null){
						nombreAtributo=attNombre.getNodeValue();
						validarNombreElementos(nombreAtributo, nListAux.item(i));
					}
					String attid = attributes.getNamedItem("xmi.id").getNodeValue();

					//valida que el atributo tenga un tipo valido
					Node attrType=attributes.getNamedItem("type");
					String atttipo = "";
					if(attrType!=null){
						atttipo=attributes.getNamedItem("type").getNodeValue();
						if(atttipo.equals("")){
							ErroresTipoAtributos.add(nListAux.item(i));
						}else{
							ValidarTipoAtributo(atttipo,nListAux.item(i));
						}
					}else{
						ErroresTipoAtributos.add(nListAux.item(i));
					}
					Nodo atributo = new Nodo(nombreAtributo, attid, "Atributo",nListAux.item(i));
					atributo.setTipo(atttipo);
					//Agregando el Atributo a la Clase
					clase.getAtributos().add(atributo);
				}

				nListAux = eElement.getElementsByTagName("UML:Operation");
				//Busqueda del subElemento Operation
				for (int j = 0; j < nListAux.getLength(); j++){
					attributes=nListAux.item(j).getAttributes();

					Node opName = attributes.getNamedItem("name");
					String nombreOperacion="";
					if(opName!=null){
						nombreOperacion=opName.getNodeValue();
						validarNombreElementos(nombreOperacion, nListAux.item(j));
					}
					String opId = attributes.getNamedItem("xmi.id").getNodeValue();
					Nodo operacion = new Nodo(nombreOperacion, opId, "Operacion", nListAux.item(j));
					Element eOperacion = (Element) nListAux.item(j);
					//Revisi�n de Par�metros y Return de la operaci�n
					NodeList nListParameters = eOperacion.getElementsByTagName("UML:Parameter");
					//Busqueda por el subsubElemento Parameter
					for (int k = 0; k < nListParameters.getLength(); k++){

						NamedNodeMap attributesParam=nListParameters.item(k).getAttributes();
						//Revisi�n si se trata de un par�metro
						if (attributesParam.getNamedItem("name") != null){

							//Creaci�n del nodo Parametro
							Node attName = attributesParam.getNamedItem("name");
							String parName="";
							if(attName!=null){
								parName=attName.getNodeValue();
								validarNombreElementos(parName, nListParameters.item(k));
							}
							String parId = attributesParam.getNamedItem("xmi.id").getNodeValue();
							String parType = attributesParam.getNamedItem("type").getNodeValue();
							Nodo parametro = new Nodo(parName, parId, "Parametro",nListParameters.item(k));
							parametro.setTipo(parType);
							//Agregando el Par�metro a la Operaci�n
							operacion.getParametros().add(parametro);
						}
						/*
						//Revisi�n si se trata de un Return, 
						 solo especifica el tipo de retrno del metodo 
						if (attributesParam.getNamedItem("name") == null){
							//Creci�n del nodo Return
							String parId = attributesParam.getNamedItem("xmi.id").getNodeValue();
							String parType = attributesParam.getNamedItem("type").getNodeValue();
							String parName = "Return";
							Nodo parametro = new Nodo(parName, parId, "Return",nListParameters.item(k)); 
							parametro.setTipo(parType);
							//Agregando el Return a la Operaci�n
							//operacion.getParametros().add(parametro);
						}*/
					}
					clase.getOperaciones().add(operacion);
				}
			}catch (NullPointerException e){
				//				System.out.println("Sin Operaciones");
			}
			clases.add(clase);
		}		
	}
	/**
	 * metodo para validar que el tipo del atributo se encuentre dentro las definiciones del 
	 * dataType
	 * @param valor del attributo para validar que se encuentre dentro de los dataTypes  
	 * @param node Referencia al nodo que representa el atributo dentro del archivo Xmi
	 */
	public void ValidarTipoAtributo(String valor,Node node){
		String tipo=BuscarDataType(valor);
		if(tipo.equals("")){
			ErroresTipoValidoAtributos.add(node);
		}
	}
	/**
	 * Método para validar que los nombres de los elementos cumplan con el estandar
	 * @param nombre nombre del elemento
	 * @param nodo referencia del elemento en el archivo Xmi
	 */
	public void validarNombreElementos(String nombre, Node nodo){

		if(!nombre.matches("^[a-zA-Z][a-zA-Z0-9]*")){
			ErroresNombreInvalidos.add(nodo);
		}
		if(palabrasReservadas.contains(nombre)){
			ErroresPalabrasReservadas.add(nodo);
		}
	}
	/************
	  PACKAGES
	 ************/
	private void ArmarPackages(Document doc) {
		NodeList nList = doc.getElementsByTagName("UML:Package");

		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			NamedNodeMap attributes=nNode.getAttributes();
			//Creaci�n del Nodo
			String name = attributes.getNamedItem("name").getNodeValue();
			String id = attributes.getNamedItem("xmi.id").getNodeValue();
			packages.put(id,new Nodo(name, id, "Paquete",nNode));
		}
	}

	/************
	  DATATYPES
	 ************/
	private void ArmarDataTypes(Document doc) {
		NodeList nList = doc.getElementsByTagName("UML:DataType");

		//Busqueda por DataType
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			NamedNodeMap attributes=nNode.getAttributes();
			//Creaci�n del Nodo
			String name = attributes.getNamedItem("name").getNodeValue();
			String id = attributes.getNamedItem("xmi.id").getNodeValue();
			dataType.put(id,new Nodo(name, id, "DataType",nNode));
		}
	}

	public String BuscarPackage(String id){
		Nodo n = packages.get(id);
		if (n != null) {
			return n.getNombre();
		}
		return "";
	}

	public String BuscarDataType(String id){
		Nodo n = dataType.get(id);
		if (n != null) {
			return n.getNombre();
		}
		return "";
	}

	/************
	 CONTAINMENT
	 ************/
	//PREGUNTAR 
	public void RelationContainment(Document doc){
		NodeList nList = doc.getElementsByTagName("UML:Class");

		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			Element eElement = (Element) nNode;
			try {
				String padreId = nNode.getAttributes().getNamedItem("xmi.id").getNodeValue();
				Element eElement2 = (Element) eElement.getChildNodes();

				for (int j = 0; j < eElement.getElementsByTagName("UML:Class").getLength(); j++){
					if (eElement2.getElementsByTagName("UML:Class").item(j).getAttributes().getNamedItem("xmi.id") != null){
						String hijoId = eElement2.getElementsByTagName("UML:Class").item(j).getAttributes().getNamedItem("xmi.id").getNodeValue();
						System.out.println("Relacion Containment: Padre = " + nNode.getAttributes().getNamedItem("name").getNodeValue() + " Hijo = " + eElement2.getElementsByTagName("UML:Class").item(j).getAttributes().getNamedItem("name").getNodeValue());
						relaciones.add(new Relacion(padreId, hijoId, "Containment"));
					}
				}
			}catch (NullPointerException e){
				//				System.out.println("No tiene Relaci�n de Containment");
			}


		}

	}

	/************
	 DEPENDENCY
	 ************/
	private void RelacionDependencia(Document doc) {
		NodeList nList = doc.getElementsByTagName("UML:Dependency");

		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			NamedNodeMap attributes=nNode.getAttributes();
			//Creaci�n de la relaci�n
			Node suplier = attributes.getNamedItem("supplier");
			Node client = attributes.getNamedItem("client");
			if(suplier!=null&&client!=null){
				relaciones.add(new Relacion(suplier.getNodeValue(), client.getNodeValue(), "Dependency"));	
			}else{
				ErroresRelaciones.add(nNode);
			}
		}
	}


	/************
    GENERALIZATION
	 ************/
	private void RelacionGeneralizacion(Document doc) {
		NodeList nList = doc.getElementsByTagName("UML:Generalization");

		//Busqueda por DataType
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			NamedNodeMap attributes=nNode.getAttributes();
			//Creaci�n de la relaci�n
			Node child =attributes.getNamedItem("child");
			Node parent =attributes.getNamedItem("parent");
			if(child!=null&&parent!=null){
				relaciones.add(new Relacion(child.getNodeValue(), parent.getNodeValue(), "Generalization"));	
			}else{
				ErroresRelaciones.add(nNode);
			}
		}
	}

	/************
	 ASSOCIATION
	 ************/
	public void RelationAsociation(Document doc){
		NodeList nList = doc.getElementsByTagName("UML:Association");

		//Busqueda por cada relaci�n Asociacion
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			Element eElement = (Element) nNode;
			//Busqueda del subElemento Attribute
			NodeList nListNodos=eElement.getElementsByTagName("UML:AssociationEnd");
			if(nListNodos!=null&&nListNodos.getLength()==2){
				Node idClase1=nListNodos.item(0).getAttributes().getNamedItem("type");
				Node idClase2=nListNodos.item(1).getAttributes().getNamedItem("type");
				if(idClase1!=null&&idClase2!=null){
					relaciones.add(new Relacion(idClase1.getNodeValue(), idClase2.getNodeValue(), "Generalization"));	
				}else{
					ErroresRelaciones.add(nNode);
				}
			}else {
				ErroresRelaciones.add(nNode);
			}
			
		}
	}
	
	private void initPalabrasReservadas(){
		palabrasReservadas.add("String");
		palabrasReservadas.add("void");
		palabrasReservadas.add("int");
		palabrasReservadas.add("private");
		palabrasReservadas.add("public");
		palabrasReservadas.add("if");
		palabrasReservadas.add("class");
		palabrasReservadas.add("return");
	}
	
	public ArrayList<Nodo> getClases() {
		return clases;
	}

	public ArrayList<Node> getErroresNombre() {
		return ErroresNombre;
	}

	public ArrayList<Node> getErroresTipoAtributos() {
		return ErroresTipoAtributos;
	}

	public ArrayList<Node> getErroresTipoValidoAtributos() {
		return ErroresTipoValidoAtributos;
	}

	public ArrayList<Node> getErroresNombreInvalidos() {
		return ErroresNombreInvalidos;
	}

	public ArrayList<Node> getErroresRelaciones() {
		return ErroresRelaciones;
	}

	public ArrayList<Node> getErroresPalabrasReservadas() {
		return ErroresPalabrasReservadas;
	}
	
	
}