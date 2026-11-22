package com.caradchecker.springbatch.rest.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;


@RestController
@RequestMapping("/api")
public class ApiController {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

    
    // Injection des dépendances par constructeur
    public ApiController(ObjectMapper objectMapper, NamedParameterJdbcTemplate jdbcTemplate) {
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate; 
    }
	
	@GetMapping("/codecs")
    public ResponseEntity<Map<String, Object>> codecs() {
		List<Map<String, Object>> codecs = new ArrayList<Map<String, Object>>();
		long start = System.currentTimeMillis();
        Map<String, Object> response = new HashMap<>();
        try {
            // Parsing initial sous forme d'arbre de nœuds JSON (ultra-rapide et sécurisé)

            Map<String, Object> toReturn = new HashMap<>();
            
            List<Map<String, Object>> segments = jdbcTemplate.query("SELECT device_id, segment_id, segment, entity.entity FROM devices_segments INNER JOIN segments ON segments.id = segment_id LEFT JOIN entity ON segments.entity_id = entity.id",new RowMapper<Map<String, Object>>( ) {
				@Override
				public Map<String, Object> mapRow(ResultSet rs, int arg1) throws SQLException {
					Map<String, Object> segment = new HashMap<String, Object>();
					segment.put("device_id", rs.getInt("device_id"));
					segment.put("segment_id", rs.getInt("segment_id"));
					segment.put("segment", rs.getString("segment"));
					segment.put("entity", rs.getString("entity"));
					return segment;
				}	 
			 });
            
            response.put("status", "ok");
            response.put("totalCodecs", codecs != null ? codecs.size() : 0); 
            response.put("codecs", codecs);
            response.putAll(toReturn);
            Map<String, Object> res = new HashMap<String, Object>();
            res.put("result", response);
            return ResponseEntity.ok(res);
        }
        catch (Exception e) {
            log.error("Erreur lors du traitement du heartbeat Cisco", e);
            return ResponseEntity.status(500).body(response);
        }
        
        finally {
       	 	long end = System.currentTimeMillis();
       	 	log.info("Called API: /apis/codecs. Returned "+codecs.size()+" codecs. Took:"+(end-start)+" millis"); 
        }
	}
	
	
	
}


