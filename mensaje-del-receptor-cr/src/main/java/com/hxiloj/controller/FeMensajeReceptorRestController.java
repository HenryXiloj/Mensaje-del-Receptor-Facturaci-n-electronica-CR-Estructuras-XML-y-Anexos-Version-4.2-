package com.hxiloj.controller;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxiloj.dto.FeMensajeReceptor;
import com.hxiloj.dto.ObligadoTributario;
import com.hxiloj.dto.Respuesta;
import com.hxiloj.hacienda.receptor.dto.MensajeReceptor;
import com.hxiloj.util.Util;

/**
 * @author hxiloj
 */

@RestController
@RequestMapping("/ws")
public class FeMensajeReceptorRestController {

	ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private MessageSource messageSource;

	@GetMapping("/obtenerResultado")
	public Respuesta obtenerResultado() {

		try {

			Respuesta respuesta = new Respuesta();
			JSONParser parser = new JSONParser();
			FeMensajeReceptor feMensajeReceptor = new FeMensajeReceptor();
			StringBuffer buf = new StringBuffer();

			File cer = new ClassPathResource("certificados/TU_CERTIFICADO.p12").getFile();

			MensajeReceptor msj = new MensajeReceptor();
			msj.setClave("LA_CLAVE");
			msj.setNumeroCedulaEmisor("LA_CEDULA_DEL_EMISOR");
			msj.setFechaEmisionDoc(Util.getFechaXmlGregoriano());
			msj.setMensaje(new BigDecimal(1).toBigInteger()); // ver documentacion de hacienda CR :). 1, 2, 3
			msj.setMontoTotalImpuesto(new BigDecimal(0).setScale(5, RoundingMode.HALF_UP)); // opcional ver
																							// documentacion hacienda CR
			msj.setTotalFactura(new BigDecimal(0).setScale(5, RoundingMode.HALF_UP));
			msj.setNumeroCedulaReceptor("LA_CEDULA_DEL_RECEPTOR");

			// valores: 1 (aceptado), 2 (aceptado parcialmente) y 3 (rechazado)
			// ver documentacion de CR
			if (msj.getMensaje().intValue() == 1) {

				buf.append("TU_NUMERO_CONSECUTIVO");
				msj.setNumeroConsecutivoReceptor(buf.replace(8, 10, "05").toString());
			} else if (msj.getMensaje().intValue() == 2) {
				buf.append("TU_NUMERO_CONSECUTIVO");
				msj.setNumeroConsecutivoReceptor(buf.replace(8, 10, "06").toString());
			} else if (msj.getMensaje().intValue() == 3) {
				buf.append("TU_NUMERO_CONSECUTIVO");
				msj.setNumeroConsecutivoReceptor(buf.replace(8, 10, "07").toString());
			}

			// Firmar xml certificado + contraseña
			String xmlF = Util.sign(messageSource.getMessage("schema.mensaje.receptor", null, Locale.getDefault()),
					cer.getPath(), messageSource.getMessage("keyPassword", null, Locale.getDefault()), msj);

			feMensajeReceptor.setComprobanteXml(Util.getFileBase64(xmlF));
			feMensajeReceptor.setClave(msj.getClave());
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			feMensajeReceptor.setFecha(format.format(new Date()));

			ObligadoTributario emisor = new ObligadoTributario();
			emisor.setTipoIdentificacion("TIPO_IDENTIFICACION");
			emisor.setNumeroIdentificacion("LA_CEDULA_DEL_EMISOR");

			ObligadoTributario receptor = new ObligadoTributario();
			receptor.setTipoIdentificacion("TIPO_IDENTIFICACION");
			receptor.setNumeroIdentificacion("LA_CEDULA_DEL_RECEPTOR");

			feMensajeReceptor.setEmisor(emisor);
			feMensajeReceptor.setReceptor(receptor);

			feMensajeReceptor.setConsecutivoReceptor(msj.getNumeroConsecutivoReceptor());

			String json = objectMapper.writeValueAsString(feMensajeReceptor);

			// Datos proporcionados por hacienda
			String token = Util.getToken(messageSource.getMessage("end.point.auth2", null, Locale.getDefault()),
					messageSource.getMessage("client_id", null, Locale.getDefault()),
					messageSource.getMessage("username", null, Locale.getDefault()),
					messageSource.getMessage("password", null, Locale.getDefault()));

			String resp = Util.respuestaRest(json, messageSource.getMessage("urlRecepcion", null, Locale.getDefault()),
					token);

			Object parseJson = parser.parse(resp);

			JSONObject jsonObject = (JSONObject) parseJson;

			Long code = (Long) jsonObject.get("X-Http-Status");

			if (code != 202) {
				System.out.println("ERROR " + jsonObject.get("X-Error-Cause").toString());
				respuesta.setRespuesta(jsonObject.get("X-Error-Cause").toString());

			} else {

				// location se encuentra la respuesta url + clave + consecutivo
				String location = (String) jsonObject.get("Location");
				location = location.replace("\\", "");
				System.out.println("location " + location);

				String strJson = Util.busqueda(location, token);

				parseJson = parser.parse(strJson);
				jsonObject = (JSONObject) parseJson;
				String strXml = (String) jsonObject.get("xml");
				String strjson = (String) jsonObject.get("json");
				String headers = (String) jsonObject.get("headers");

				respuesta.setRespuesta("XML -> " + strXml + " JSON -> " + strjson + "headers --> " + headers);
			}

			return respuesta;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
