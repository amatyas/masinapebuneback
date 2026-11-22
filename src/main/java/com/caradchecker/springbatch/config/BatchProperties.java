package com.caradchecker.springbatch.config;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.caradchecker.springbatch.util.CryptoHelper;

public class BatchProperties {
	
	private static final Logger log = LoggerFactory.getLogger(BatchProperties.class);
	private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(5);
	private static final Object CACHE_LOCK = new Object();

	private static volatile CachedProperties cache;

	public static boolean isDemo(JdbcTemplate jdbcTemplate) {
		return "1".equals(StringUtils.trimToEmpty(getProperties(jdbcTemplate).get(Properties.IS_DEMO.key())));
	}

	public static Map<String, String> getProperties(JdbcTemplate jdbcTemplate) {
		CachedProperties current = cache;
		if (current != null && !current.isExpired()) {
			return copyOf(current.properties);
		}
		synchronized (CACHE_LOCK) {
			current = cache;
			if (current != null && !current.isExpired()) {
				return copyOf(current.properties);
			}
			Map<String, String> loaded = Collections.unmodifiableMap(new HashMap<String, String>(loadProperties(jdbcTemplate)));
			cache = new CachedProperties(loaded, System.currentTimeMillis());
			return copyOf(loaded);
		}
	}

	private static Map<String, String> copyOf(Map<String, String> properties) {
		return new HashMap<String, String>(properties);
	}

	private static final class CachedProperties {
		private final Map<String, String> properties;
		private final long loadedAtMillis;

		private CachedProperties(Map<String, String> properties, long loadedAtMillis) {
			this.properties = properties;
			this.loadedAtMillis = loadedAtMillis;
		}

		private boolean isExpired() {
			return System.currentTimeMillis() - loadedAtMillis >= CACHE_TTL_MS;
		}
	}

	public static Map<String, String> loadProperties(JdbcTemplate jdbcTemplate) {
		log.info("Loading batch properties");
		List<Map<String, String>> properties = jdbcTemplate.query("SELECT * FROM batch_properties",new RowMapper<Map<String, String>>( ) {
			@Override
			public Map<String, String> mapRow(ResultSet rs, int arg1) throws SQLException {
				Map<String, String> config = new HashMap<String, String>();
		        config.put("key", rs.getString("property"));
		        config.put("value", rs.getString("value"));
				//log.info(config.toString());
				return config;
			}	 
		 });
		
		// add server properties
		List<Map<String, String>> propertiesServer = jdbcTemplate.queryForObject("SELECT server_url, client_profile, is_visiologic_OBTP_enable, is_demo FROM configuration WHERE id=1 ",new RowMapper<List<Map<String, String>>>( ) {
			@Override
			public List<Map<String, String>> mapRow(ResultSet rs, int arg1) throws SQLException {
				List<Map<String, String>> list = new ArrayList<Map<String, String>>();
				Map<String, String> config = new HashMap<String, String>();
		        config.put("key", Properties.SERVER_URL.key());
		        config.put("value", rs.getString("server_url"));
		        list.add(config);
		        
		        Map<String, String> config1 = new HashMap<String, String>();
		        config1.put("key", Properties.CLIENT_PROFILE.key());
		        config1.put("value", rs.getString("client_profile"));
		        list.add(config1);
		        
		        Map<String, String> config2 = new HashMap<String, String>();
		        config2.put("key", Properties.IS_VISIOLOGIC_OBTP_ENABLE.key());
		        config2.put("value", rs.getString("is_visiologic_OBTP_enable"));
		        list.add(config2);

		        Map<String, String> config3 = new HashMap<String, String>();
		        config3.put("key", Properties.IS_DEMO.key());
		        config3.put("value", rs.getString("is_demo"));
		        list.add(config3);
				return list;
			}	 
		});
		if(propertiesServer != null && propertiesServer.size()>0) {
			for (Map<String, String> map : propertiesServer) {
				properties.add(map);
			}
		}
		
		
		try {
			List<Map<String, String>> propertiesEmail = jdbcTemplate.queryForObject("SELECT host, port, alias, username, password, type_hash, secret_key, disable_auth FROM configuration_mail WHERE id=1 ",new RowMapper<List<Map<String, String>>>( ) {
				@Override
				public List<Map<String, String>> mapRow(ResultSet rs, int arg1) throws SQLException {
					List<Map<String, String>> list = new ArrayList<Map<String, String>>();
					Map<String, String> config = new HashMap<String, String>();
					
			        config.put("key", Properties.SMTP_HOST.key());
			        config.put("value", rs.getString("host"));
			        list.add(config);
			        
			        Map<String, String> config1 = new HashMap<String, String>();
			        config1.put("key", Properties.SMTP_PORT.key());
			        config1.put("value", rs.getString("port"));
			        list.add(config1);
			        
			        Map<String, String> config2 = new HashMap<String, String>();
			        config2.put("key", Properties.SMTP_ALIAS.key());
			        config2.put("value", rs.getString("alias"));
			        list.add(config2);
			        
			        Map<String, String> config3 = new HashMap<String, String>();
			        config3.put("key", Properties.SMTP_USERNAME.key());
			        config3.put("value", rs.getString("username"));
			        list.add(config3);
			        
			        String decodedPassword = null;
			        String encodedPassword = rs.getString("password");
			        String typeHash = rs.getString("type_hash");
			        String secret = rs.getString("secret_key");
			        
			        if(encodedPassword != null && !StringUtils.isBlank(encodedPassword)) {
			        	try {
							decodedPassword = CryptoHelper.decrypt(encodedPassword, secret);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							log.error("Unable to decode smtp password", e);
						}
			        }
			        
			        Map<String, String> config4 = new HashMap<String, String>();
			        config4.put("key", Properties.SMTP_PASS.key());
			        config4.put("value", decodedPassword);
			        list.add(config4);
			        
			        Map<String, String> config5 = new HashMap<String, String>();
			        config5.put("key", Properties.SMTP_DISABLE_AUTH.key());
			        config5.put("value", rs.getString("disable_auth"));
			        list.add(config5);
			        
			        
					return list;
				}	 
			});
			if(propertiesEmail != null && propertiesEmail.size()>0) {
				for (Map<String, String> map : propertiesEmail) {
					properties.add(map);
				}
			}
		} catch(Exception e) {}
		
		try {
			List<Map<String, String>> propertiesProxy = jdbcTemplate.queryForObject("SELECT id, url, type, user, password, type_hash, secret_key FROM configuration_proxy WHERE id = 1",new RowMapper<List<Map<String, String>>>( ) {
				@Override
				public List<Map<String, String>> mapRow(ResultSet rs, int arg1) throws SQLException {
					List<Map<String, String>> list = new ArrayList<Map<String, String>>();
					
					String proxyUrl = rs.getString("url");
					
			        Map<String, String> config1 = new HashMap<String, String>();
			        config1.put("key", Properties.PROXY_TYPE.key());
			        config1.put("value", rs.getString("type"));
			        list.add(config1);
			        
			        Map<String, String> config2 = new HashMap<String, String>();
			        config2.put("key", Properties.PROXY_USER.key());
			        config2.put("value", rs.getString("user"));
			        list.add(config2);
			        
			        String decodedPassword = null;
			        String encodedPassword = rs.getString("password");
			        String typeHash = rs.getString("type_hash");
			        String secret = rs.getString("secret_key");
			        
			        if(encodedPassword != null && !StringUtils.isBlank(encodedPassword)) {
			        	try {
							decodedPassword = CryptoHelper.decrypt(encodedPassword, secret);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							log.error("Unable to decode smtp password", e);
						}
			        }
			        
			        String proxyHost = null;
			        String proxyPort = "80";
					if(!StringUtils.isBlank(proxyUrl)) {
						proxyUrl = proxyUrl.toLowerCase();
						proxyUrl = proxyUrl.replaceAll("https://", "");
						proxyUrl = proxyUrl.replaceAll("http://", "");
						String[] proxyUrlArray = proxyUrl.split(":");
						if(proxyUrlArray.length>0) {
							proxyHost = proxyUrlArray[0];
							if(proxyUrlArray.length>1) {
								proxyPort = proxyUrlArray[1];
							}
						}
						
					}
					
			        Map<String, String> config3 = new HashMap<String, String>();
			        config3.put("key", Properties.PROXY_PASS.key());
			        config3.put("value", decodedPassword);
			        list.add(config3);
					
					Map<String, String> config4 = new HashMap<String, String>();
			        config4.put("key", Properties.PROXY_HOST.key());
			        config4.put("value", proxyHost);
			        list.add(config4);
			        
			        Map<String, String> config5 = new HashMap<String, String>();
			        config5.put("key", Properties.PROXY_PORT.key());
			        config5.put("value", proxyPort);
			        list.add(config5);

					return list;
				}	 
			});
			if(propertiesProxy != null && propertiesProxy.size()>0) {
				for (Map<String, String> map : propertiesProxy) {
					properties.add(map);
				}
			}
		} catch(Exception e) {}

		try {
			String engineType = jdbcTemplate.queryForObject("SELECT engine_type FROM configuration_ai WHERE id = 1", String.class);
			if (StringUtils.isBlank(engineType)) {
				engineType = "ollama";
			}
			Map<String, String> configEngine = new HashMap<String, String>();
			configEngine.put("key", Properties.AI_ENGINE_TYPE.key());
			configEngine.put("value", engineType.trim());
			properties.add(configEngine);
		} catch(Exception e) {}
		
		try {
			List<Map<String, String>> propertiesProxy = jdbcTemplate.queryForObject("SELECT id, ollama_base_url, ollama_model, preload_model, model_keep_alive_sec, ollama_online_api_key FROM configuration_ai WHERE id = 1",new RowMapper<List<Map<String, String>>>( ) {
				@Override
				public List<Map<String, String>> mapRow(ResultSet rs, int arg1) throws SQLException {
					List<Map<String, String>> list = new ArrayList<Map<String, String>>();
					
			        Map<String, String> config1 = new HashMap<String, String>();
			        config1.put("key", Properties.OLLAMA_BASE_URL.key());
			        config1.put("value", rs.getString("ollama_base_url"));
			        list.add(config1);
			        
			        Map<String, String> config2 = new HashMap<String, String>();
			        config2.put("key", Properties.OLLAMA_MODEL.key());
			        config2.put("value", rs.getString("ollama_model"));
			        list.add(config2);
			        
			        String prealoadModel = rs.getString("preload_model"); 
			        if(StringUtils.isBlank(prealoadModel)) {
			        	prealoadModel = "1";
			        }
			        
			        Map<String, String> config3 = new HashMap<String, String>();
			        config3.put("key", Properties.OLLAMA_PRELOAD.key());
			        config3.put("value", prealoadModel);
			        list.add(config3);
					
			        String keepAlive = rs.getString("model_keep_alive_sec"); 
			        if(StringUtils.isBlank(keepAlive)) {
			        	keepAlive = "-1";
			        }
			        
			        Map<String, String> config4 = new HashMap<String, String>();
			        config4.put("key", Properties.OLLAMA_KEEP_ALIVE.key());
			        config4.put("value", keepAlive);
			        list.add(config4);

			        Map<String, String> config5 = new HashMap<String, String>();
			        config5.put("key", Properties.OLLAMA_API_KEY.key());
			        config5.put("value", rs.getString("ollama_online_api_key"));
			        list.add(config5);

					return list;
				}	 
			});
			if(propertiesProxy != null && propertiesProxy.size()>0) {
				for (Map<String, String> map : propertiesProxy) {
					properties.add(map);
				}
			}
		} catch(Exception e) {}
		
		
		Map<String, String> mapToReturn = new HashMap<String, String>();
		if(properties != null) {
			for (Map<String, String> map : properties) {
				mapToReturn.put(map.get("key"), map.get("value"));
				if(map.get("key").contains("password") || map.get("key").contains("api-key")) {
					log.debug(map.get("key")+"=******");
				}
				else {
					log.debug(map.get("key")+"="+map.get("value"));
				}
			}
		}
		
		return mapToReturn;
	}
}
