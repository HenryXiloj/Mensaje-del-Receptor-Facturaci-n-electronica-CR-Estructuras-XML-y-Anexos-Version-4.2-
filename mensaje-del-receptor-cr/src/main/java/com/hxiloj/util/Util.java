package com.hxiloj.util;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import xades4j.production.Enveloped;
import xades4j.production.XadesEpesSigningProfile;
import xades4j.production.XadesSigner;
import xades4j.production.XadesSigningProfile;
import xades4j.properties.ObjectIdentifier;
import xades4j.properties.SignaturePolicyBase;
import xades4j.properties.SignaturePolicyIdentifierProperty;
import xades4j.providers.KeyingDataProvider;
import xades4j.providers.SignaturePolicyInfoProvider;
import xades4j.providers.impl.FileSystemKeyStoreKeyingDataProvider;

/**
 * @author hxiloj
 */
public class Util {

	ObjectMapper objectMapper = new ObjectMapper();

	public static <T> String convertObjectToXML(T object) {
		try {
			StringWriter stringWriter = new StringWriter();
			JAXBContext context = JAXBContext.newInstance(object.getClass());
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(object, stringWriter);
			return stringWriter.toString();
		} catch (JAXBException e) {
			throw new RuntimeException(String.format("Exception while marshalling: %s", e.getMessage()));
		}

	}

	@SuppressWarnings("unchecked")
	public static <T> T convertXMLToObject(Class<T> clazz, String xml) {
		try {
			JAXBContext context = JAXBContext.newInstance(clazz);
			Unmarshaller um = context.createUnmarshaller();
			return (T) um.unmarshal(new StringReader(xml));
		} catch (JAXBException je) {
			throw new RuntimeException(String.format("Exception while Unmarshaller: %s", je.getMessage()));
		}
	}


	public static XMLGregorianCalendar getFechaXmlGregoriano() {

		GregorianCalendar fechaGregoriana = new GregorianCalendar();
		fechaGregoriana.setTime(new Date());
		XMLGregorianCalendar XmlFecha = null;
		try {
			XmlFecha = DatatypeFactory.newInstance().newXMLGregorianCalendar(fechaGregoriana);
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		return XmlFecha;

	}

	public static String getFileBase64(String xml) {
		try {

			String base64 = Base64.encodeBase64String(xml.getBytes());
			return base64;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

	public static String getToken(String urlRest, String clientId, String username, String password) {

		try {
			String token = "";
			HttpClient httpClient = HttpClientBuilder.create().build();
			HttpPost request = new HttpPost(urlRest);
			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			urlParameters.add(new BasicNameValuePair("grant_type", "password"));
			urlParameters.add(new BasicNameValuePair("client_id", clientId));
			urlParameters.add(new BasicNameValuePair("client_secret", ""));
			urlParameters.add(new BasicNameValuePair("scope", ""));
			urlParameters.add(new BasicNameValuePair("username", username));
			urlParameters.add(new BasicNameValuePair("password", password));

			request.addHeader("content-type", "application/x-www-form-urlencoded");
			request.setEntity(new UrlEncodedFormEntity(urlParameters));
			HttpResponse response = httpClient.execute(request);
			HttpEntity entity = response.getEntity();
			String responseString = EntityUtils.toString(entity, "UTF-8");
			ObjectMapper objectMapper = new ObjectMapper();
			Map<String, Object> res = objectMapper.readValue(responseString, new TypeReference<Map<String, Object>>() {
			});
			token = (String) res.get("access_token");
			return token;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public static String respuestaRest(String json, String urlRest, String token) {

		try {

			HttpClient httpClient = HttpClientBuilder.create().build();
			HttpPost request = new HttpPost(urlRest);

			StringEntity params = new StringEntity(json);
			request.addHeader("content-type", "application/javascript");
			request.addHeader("Authorization", "bearer " + token);
			request.setEntity(params);
			HttpResponse response = httpClient.execute(request);
			return printHeaders(response.getStatusLine().getStatusCode(), response.getAllHeaders());

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static String busqueda(String endpoint, String token) {

		try {

			JSONObject json = new JSONObject();
			String respuestaXML = null;

			HttpClient httpClient = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(endpoint);
			request.addHeader("Authorization", "bearer " + token);

			HttpResponse response = httpClient.execute(request);

			HttpEntity entity = response.getEntity();
			String responseString = EntityUtils.toString(entity, "UTF-8");

			if(responseString!=null && !responseString.isEmpty()) {
				
				ObjectMapper objectMapper = new ObjectMapper();
				Map<String, Object> res = objectMapper.readValue(responseString,new TypeReference<Map<String, Object>>() {});
				respuestaXML = (String) res.get("respuesta-xml");
				respuestaXML = (respuestaXML==null?null:new String(Base64.decodeBase64(respuestaXML), "UTF-8"));				

			}

			json.put("X-Http-Status", response.getStatusLine().getStatusCode());
			json.put("json", responseString);
			json.put("xml", respuestaXML);
			json.put("headers", printHeaders(response.getStatusLine().getStatusCode(), response.getAllHeaders()));

			return json.toString();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String sign(String schema, String keyPath, String password, Object obj) {

		KeyingDataProvider kp;
		try {

			String stringXmlEntrada = convertObjectToXML(obj);

			SignaturePolicyInfoProvider policyInfoProvider = new SignaturePolicyInfoProvider() {
				public SignaturePolicyBase getSignaturePolicy() {
					return new SignaturePolicyIdentifierProperty(new ObjectIdentifier(schema),
							new ByteArrayInputStream("Politica de Factura Digital".getBytes()));
				}
			};

			kp = new FileSystemKeyStoreKeyingDataProvider("pkcs12", keyPath, new FirstCertificateSelector(),
					new DirectPasswordProvider(password), new DirectPasswordProvider(password), false);

			// SignaturePolicyInfoProvider spi = new
			XadesSigningProfile p = new XadesEpesSigningProfile(kp, policyInfoProvider);
			// p.withBasicSignatureOptionsProvider(new SignatureOptionsProvider());

			// open file
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = null;
			builder = factory.newDocumentBuilder();

			/**/
			StringReader b = new StringReader(stringXmlEntrada);
			InputSource a = new InputSource(b);
			Document doc1 = builder.parse(a);
			/**/
			// Document doc1 = builder.parse(new File (stringXmlEntrada));
			Element elemToSign = doc1.getDocumentElement();

			XadesSigner signer = p.newSigner();

			new Enveloped(signer).sign(elemToSign);

			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			Source input = new DOMSource(doc1);
			StringWriter sw = new StringWriter();

			transformer.transform(input, new StreamResult(sw));

			return sw.toString();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static String printHeaders(int statusCode, Header[] headers) {

		JSONObject json = new JSONObject();

		try {

			for (Header header : headers) {
				json.put(header.getName(), header.getValue());
			}

			json.put("X-Http-Status", statusCode);
			return json.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
