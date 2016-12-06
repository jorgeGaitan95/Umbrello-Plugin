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

	private static TreeAbstract instance;
	private static XmlSourceCode instancexml;

	private TreeAbstract(XmlSourceCode archivoXml) {
		Document document = archivoXml.getDocument(false);
		if (document.getDocumentElement() != null) {
			instancexml=archivoXml;
			init(document);
		}
	}
	/**
	 * 
	 * @param xml
	 * @return
	 */
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
			String name = nNode.getAttributes().getNamedItem("name").getNodeValue();
			String id = nNode.getAttributes().getNamedItem("xmi.id").getNodeValue();
			Nodo clase = new Nodo(name, id, "Clase");

			try {
				NodeList nListAux = eElement.getElementsByTagName("UML:Attribute");
				NamedNodeMap attributes;
				//Busqueda del subElemento Attribute
				for (int i = 0; 1 < nListAux.getLength(); i++){
					attributes=nListAux.item(i).getAttributes();
					//Creaci�n del nodo Atributo
					String attNombre = attributes.getNamedItem("name").getNodeValue();
					String attid = attributes.getNamedItem("xmi.id").getNodeValue();
					String atttipo = attributes.getNamedItem("type").getNodeValue();
					Nodo atributo = new Nodo(attNombre, attid, "Atributo");
					atributo.setTipo(atttipo);
					//Agregando el Atributo a la Clase
					clase.getAtributos().add(atributo);
				}

				nListAux = eElement.getElementsByTagName("UML:Operation");
				//Busqueda del subElemento Operation
				for (int j = 0; j < nListAux.getLength(); j++){
					attributes=nListAux.item(j).getAttributes();
					String opName = attributes.getNamedItem("name").getNodeValue();
					String opId = attributes.getNamedItem("xmi.id").getNodeValue();
					Nodo operacion = new Nodo(opName, opId, "Operacion");

					//Revisi�n de Par�metros y Return de la operaci�n
					NodeList nListParameters = nListAux.item(j).getChildNodes();
					//Busqueda por el subsubElemento Parameter
					for (int k = 0; k < nListParameters.getLength(); j++){

						NamedNodeMap attributesParam=nListParameters.item(k).getAttributes();
						//Revisi�n si se trata de un par�metro
						if (attributesParam.getNamedItem("kind") == null){

							//Creaci�n del nodo Parametro
							String parName = attributesParam.getNamedItem("name").getNodeValue();
							String parId = attributesParam.getNamedItem("xmi.id").getNodeValue();
							String parType = attributesParam.getNamedItem("prQYKnCn9SJd").getNodeValue();
							Nodo parametro = new Nodo(parName, parId, "Parametro");
							parametro.setTipo(parType);
							//Agregando el Par�metro a la Operaci�n
							operacion.getParametros().add(parametro);
						}

						//Revisi�n si se trata de un Return 
						if (attributesParam.getNamedItem("name") == null){
							//Creci�n del nodo Return
							String parId = attributesParam.getNamedItem("xmi.id").getNodeValue();
							String parType = attributesParam.getNamedItem("type").getNodeValue();
							String parName = "Return";
							Nodo parametro = new Nodo(parName, parId, "Return"); 
							parametro.setTipo(parType);
							//Agregando el Return a la Operaci�n
							operacion.getParametros().add(parametro);
						}

					}
					clase.getOperaciones().add(operacion);
				}
			}catch (NullPointerException e){
				//				System.out.println("Sin Operaciones");
			}
			clases.add(clase);
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
			packages.put(id,new Nodo(name, id, "Paquete"));
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
			dataType.put(id,new Nodo(name, id, "DataType"));
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
		Nodo n = packages.get(id);
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
			try {
				//Creaci�n de la relaci�n
				String suplier = attributes.getNamedItem("supplier").getNodeValue();
				String client = attributes.getNamedItem("client").getNodeValue();
				relaciones.add(new Relacion(suplier, client, "Dependency"));

			}catch (NullPointerException e){
				//			System.out.println("No tiene Relaci�n de Containment");
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
			try {
				//Creaci�n de la relaci�n
				String child =attributes.getNamedItem("child").getNodeValue();
				String parent =attributes.getNamedItem("parent").getNodeValue();
				relaciones.add(new Relacion(child, parent, "Generalization"));
			}catch (NullPointerException e){
				//			System.out.println("No tiene Relaci�n de Containment");
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
			try {
				//Busqueda del subElemento Attribute
				NodeList nListNodos=eElement.getElementsByTagName("UML:AssociationEnd");
				String idClase1=nListNodos.item(0).getAttributes().getNamedItem("type").getNodeValue();
				String idClase2=nListNodos.item(1).getAttributes().getNamedItem("type").getNodeValue();
				relaciones.add(new Relacion(idClase1, idClase2, "Generalization"));
			}catch (NullPointerException e){
				//			System.out.println("No hay mas Atributos");
			}
		}

	}

}