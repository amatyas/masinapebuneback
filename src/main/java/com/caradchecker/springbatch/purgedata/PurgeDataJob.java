package com.caradchecker.springbatch.purgedata;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import com.caradchecker.springbatch.config.BatchProperties;
import com.caradchecker.springbatch.config.Properties;

@Configuration
public class PurgeDataJob {
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private JobRepository jobRepository;
	
	@Autowired
    private PlatformTransactionManager transactionManager;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;


	@Bean(name = "jobPurgeData")
	public Job job() throws Exception {
		Job job = new JobBuilder("PurgeDataJob", jobRepository).start(step()).build();
		return job;
	}

	private Step step() throws Exception {
		
		Tasklet takslet = new Tasklet() {
			@Override
			public RepeatStatus execute(StepContribution arg0, ChunkContext arg1) throws Exception {
				  
				if("main".equalsIgnoreCase(Thread.currentThread().getName())) {
					log.info("We do not purge DB at batch startup in the main thread");
				}
				else {
			
					LocalDate now = LocalDate.now();  
				    int day = now.getDayOfMonth();  
				    if(day == 1) {
				    	// first day of month we purge localisations
						String countQ = "SELECT count(id) as nb FROM workspaces WHERE id NOT IN (SELECT DISTINCT location FROM devices WHERE location IS NOT NULL)";
						String deleteQ  = "DELETE FROM workspaces WHERE id NOT IN (SELECT DISTINCT location FROM devices WHERE location IS NOT NULL)";
						String labelD = "workspaces";
						executePurge(countQ, deleteQ, labelD, 0);
						
						
						countQ = "SELECT count(id) as nb FROM building_zones WHERE id NOT IN (SELECT DISTINCT zone_id FROM workspaces WHERE zone_id IS NOT NULL)";
						deleteQ  = "DELETE FROM building_zones WHERE id NOT IN (SELECT DISTINCT zone_id FROM workspaces WHERE zone_id IS NOT NULL)";
						labelD = "building_zones";
						executePurge(countQ, deleteQ, labelD, 0);
						
						countQ = "SELECT count(id) as nb FROM buildings WHERE id not in (SELECT DISTINCT building_id FROM workspaces WHERE building_id IS NOT NULL)";
						deleteQ  = "DELETE FROM buildings WHERE id not in (SELECT DISTINCT building_id FROM workspaces WHERE building_id IS NOT NULL)";
						labelD = "buildings";
						executePurge(countQ, deleteQ, labelD, 0);
						
						countQ = "SELECT count(id) as nb FROM sites WHERE id not in (SELECT DISTINCT site_id FROM buildings WHERE site_id IS NOT NULL)";
						deleteQ  = "DELETE FROM sites WHERE id not in (SELECT DISTINCT site_id FROM buildings WHERE site_id IS NOT NULL)";
						labelD = "sites";
						executePurge(countQ, deleteQ, labelD, 0);
	
						countQ = "SELECT count(id) as nb FROM cities WHERE id not in (SELECT DISTINCT city_id FROM buildings WHERE city_id IS NOT NULL)";
						deleteQ  = "DELETE FROM cities WHERE id not in (SELECT DISTINCT city_id FROM buildings WHERE city_id IS NOT NULL)";
						labelD = "cities";
						executePurge(countQ, deleteQ, labelD, 0);
				    }
				
					Map<String, String> properties = BatchProperties.getProperties(jdbcTemplate); 
					String sMonthsPurgeCallHistory = properties.get(Properties.PURGE_CALL_HISTORY_DELAY_MONTHS.key());
					String sMonthsPurgeCronJobs = properties.get(Properties.PURGE_CRONJOBS_HISTORY_DELAY_MONTHS.key());
					String sMonthsPurgeMacrosTracking = properties.get(Properties.PURGE_MACROS_TRACKING_DELAY_MONTHS.key());
					String sMonthsPurgeRatings = properties.get(Properties.PURGE_RATINGS_DELAY_MONTHS.key());
					String sMonthsPurgeTickets = properties.get(Properties.PURGE_USER_REQUESTS_DELAY_MONTHS.key());
					String sMonthsPurgePresentations = properties.get(Properties.PURGE_PRESENTATION_HISTORY_DELAY_MONTHS.key());
					String sMonthsPurgeWebex = properties.get(Properties.PURGE_WEBEX_HISTORY_DELAY_MONTHS.key());
					String sMonthsPurgeMsTeams = properties.get(Properties.PURGE_MSTEAMS_HISTORY_DELAY_MONTHS.key());
					String sJabberPurgeMsTeams = properties.get(Properties.PURGE_JABBER_HISTORY_DELAY_MONTHS.key());
					String sMonthsPurgeHeartbeatHistory = properties.get(Properties.PURGE_HEARTBEAT_HISTORY_DELAY_MONTHS.key());
					String sMonthsPurgeCodecDownHistory = properties.get(Properties.PURGE_CODECDOWNTIME_HISTORY_DELAY_MONTHS.key());
					
					Integer monthsPurgeCallHistory = null;
					if(sMonthsPurgeCallHistory!= null && !sMonthsPurgeCallHistory.isEmpty()) {
						try { monthsPurgeCallHistory = Integer.parseInt(sMonthsPurgeCallHistory); } catch(Exception e) {log.error("Unexpected exception", e);}
					}
					
					Integer monthsPurgeHeartbeatHistory = null;
					if(sMonthsPurgeHeartbeatHistory!= null && !sMonthsPurgeHeartbeatHistory.isEmpty()) {
						try { monthsPurgeHeartbeatHistory = Integer.parseInt(sMonthsPurgeHeartbeatHistory); } catch(Exception e) {log.error("Unexpected exception", e);}
					}
					
					Integer monthsPurgeCodecDownTimeHistory = null;
					if(sMonthsPurgeCodecDownHistory!= null && !sMonthsPurgeCodecDownHistory.isEmpty()) {
						try { monthsPurgeCodecDownTimeHistory = Integer.parseInt(sMonthsPurgeCodecDownHistory); } catch(Exception e) {log.error("Unexpected exception", e);}
					}
					
					Integer monthsPurgeCronJobs = null;
					if(sMonthsPurgeCronJobs!= null && !sMonthsPurgeCronJobs.isEmpty()) {
						try { monthsPurgeCronJobs = Integer.parseInt(sMonthsPurgeCronJobs); } catch(Exception e) {log.error("Unexpected exception", e);}
					}
					
					Integer monthsPurgeMacrosTracking = null;
					if(sMonthsPurgeMacrosTracking!= null && !sMonthsPurgeMacrosTracking.isEmpty()) {
						try { monthsPurgeMacrosTracking = Integer.parseInt(sMonthsPurgeMacrosTracking); } catch(Exception e) {log.error("Unexpected exception", e);}
					}
					
					Integer monthsPurgeRatings = null;
					if(sMonthsPurgeRatings!= null && !sMonthsPurgeRatings.isEmpty()) {
						try { monthsPurgeRatings = Integer.parseInt(sMonthsPurgeRatings); } catch(Exception e) {log.error("Unexpected exception", e);}
					}
					
					Integer monthsPurgeTickets = null;
					if(sMonthsPurgeTickets!= null && !sMonthsPurgeTickets.isEmpty()) {
						try { monthsPurgeTickets = Integer.parseInt(sMonthsPurgeTickets); } catch(Exception e) {log.error("Unexpected exception", e);}
					}
					
					Integer monthsPurgePresentations = null;
					if(sMonthsPurgePresentations!= null && !sMonthsPurgePresentations.isEmpty()) {
						try { monthsPurgePresentations = Integer.parseInt(sMonthsPurgePresentations); } catch(Exception e) {log.error("Unexpected exception", e);}
					}
					
					Integer monthsPurgeWebex = null;
					if(sMonthsPurgeWebex!= null && !sMonthsPurgeWebex.isEmpty()) {
						try { monthsPurgeWebex = Integer.parseInt(sMonthsPurgeWebex); } catch(Exception e) {log.error("Unexpected exception", e);}
					}
		
					Integer monthsPurgeMsTeams = null;
					if(sMonthsPurgeMsTeams!= null && !sMonthsPurgeMsTeams.isEmpty()) {
						try { monthsPurgeMsTeams = Integer.parseInt(sMonthsPurgeMsTeams); } catch(Exception e) {log.error("Unexpected exception", e);}
					}
					
					
					Integer monthsPurgeJabber = null;
					if(sJabberPurgeMsTeams!= null && !sJabberPurgeMsTeams.isEmpty()) {
						try { monthsPurgeJabber = Integer.parseInt(sJabberPurgeMsTeams); } catch(Exception e) {log.error("Unexpected exception", e);}
					}
					
					if(monthsPurgeCallHistory != null && monthsPurgeCallHistory.intValue()>0) {
						int monthToKeep = monthsPurgeCallHistory.intValue();
						String countQuery = "SELECT count(id) as nb FROM calls_history WHERE call_start<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String deleteQuery  = "DELETE FROM calls_history WHERE call_start<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String labelData = "Calls history";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
					}
					else {
						log.info("Calls history: purge is disabled");
					}
					
					if(monthsPurgeHeartbeatHistory != null && monthsPurgeHeartbeatHistory.intValue()>0) {
						int monthToKeep = monthsPurgeHeartbeatHistory.intValue();
						String countQuery = "SELECT count(device_id) as nb FROM devices_heartbeat_history WHERE creation_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String deleteQuery  = "DELETE FROM devices_heartbeat_history WHERE creation_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String labelData = "Heartbeat history";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
					}
					else {
						log.info("Heartbeat history: purge is disabled");
					}
					
					if(monthsPurgeCodecDownTimeHistory != null && monthsPurgeCodecDownTimeHistory.intValue()>0) {
						int monthToKeep = monthsPurgeCodecDownTimeHistory.intValue();
						String countQuery = "SELECT count(device_id) as nb FROM devices_downtime_history WHERE created_at<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String deleteQuery  = "DELETE FROM devices_downtime_history WHERE created_at<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String labelData = "CodecDownTime history";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
					}
					else {
						log.info("CodecDownTime history: purge is disabled");
					}
					
					if(monthsPurgePresentations != null && monthsPurgePresentations.intValue()>0) {
						int monthToKeep = monthsPurgePresentations.intValue();
						String countQuery = "SELECT count(id) as nb FROM presentation_history WHERE presentation_start<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String deleteQuery  = "DELETE FROM presentation_history WHERE presentation_start<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String labelData = "Presentations history";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
					}
					else {
						log.info("Presentations history: purge is disabled");
					}
					
					if(monthsPurgeWebex != null && monthsPurgeWebex.intValue()>0) {
						int monthToKeep = monthsPurgeWebex.intValue();
						String countQuery = "SELECT count(id) as nb FROM stats_webex_meetings_attendees WHERE joinTime<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String deleteQuery  = "DELETE FROM stats_webex_meetings_attendees WHERE joinTime<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String labelData = "Webex meeting attendees history";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
						
						countQuery = "SELECT count(confID) as nb FROM stats_webex_meetings WHERE meetingStartTime<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM stats_webex_meetings  WHERE meetingStartTime<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Webex meetings history";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
						
						countQuery = "SELECT count(*) as nb FROM webex_users_snapshot_daily_by_segment WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM webex_users_snapshot_daily_by_segment WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Webex users daily snapshots by segments";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
						
						countQuery = "SELECT count(*) as nb FROM webex_users_snapshot_daily_by_site WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM webex_users_snapshot_daily_by_site WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Webex users daily snapshots by site";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
						
						countQuery = "SELECT count(*) as nb FROM webex_clients_snapshot_daily_by_site WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM webex_clients_snapshot_daily_by_site WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Webex users daily snapshots by client";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
						
						countQuery = "SELECT count(*) as nb FROM webex_clients_snapshot_daily_by_segment WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM webex_clients_snapshot_daily_by_segment WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Webex clients daily snapshots by segment";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
					
						countQuery = "SELECT count(*) as nb FROM webex_clients_snapshot_daily_by_site WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM webex_clients_snapshot_daily_by_site WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Webex clients daily snapshots by site";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
					
						countQuery = "SELECT count(*) as nb FROM webex_sessions_snapshot_daily_by_segment WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM webex_sessions_snapshot_daily_by_segment WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Webex sessions daily snapshots by segment";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
					
						countQuery = "SELECT count(*) as nb FROM webex_sessions_snapshot_daily_by_site WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM webex_sessions_snapshot_daily_by_site WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Webex sessions daily snapshots by site";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
					
						countQuery = "SELECT count(*) as nb FROM stats_webex_meetings_daily_quality_metrics WHERE created_at<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM stats_webex_meetings_daily_quality_metrics WHERE created_at<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Webex quality metrics daily snapshots by site";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
					}
					else {
						log.info("Webex history: purge is disabled");
					}
					
					if(monthsPurgeCronJobs != null && monthsPurgeCronJobs.intValue()>0) {
						int monthToKeep = monthsPurgeCronJobs.intValue();
						long start = System.currentTimeMillis();
						long startSelection = System.currentTimeMillis();
						List<Map<String, Integer>> cronjobsIdsToPurge = jdbcTemplate.query("SELECT id, transaction_id FROM cronjobs WHERE creation_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)",new RowMapper<Map<String, Integer>>( ) {
							@Override
							public Map<String, Integer> mapRow(ResultSet rs, int arg1) throws SQLException {
								Map<String, Integer> row = new HashMap<String, Integer>();
								row.put("id", rs.getInt("id"));
						        row.put("transaction_id", rs.getInt("transaction_id"));
								return row;
							}	 
						 });
						long endSelection = System.currentTimeMillis();
						log.info("Cronjobs data : keep only "+monthToKeep+" month(s). Found: "+ (cronjobsIdsToPurge != null ? cronjobsIdsToPurge.size() : 0) + " records to delete. Selection took:"+(endSelection-startSelection)+" millis");
						if(cronjobsIdsToPurge != null && cronjobsIdsToPurge.size()>0) {
							// deleting cronjobs_details table
							long startCronjobDetails = System.currentTimeMillis();
							jdbcTemplate.batchUpdate(
							 	"DELETE FROM cronjobs_details WHERE job_id=?",
							 	cronjobsIdsToPurge,
							 	1024,
				                new ParameterizedPreparedStatementSetter<Map<String, Integer>>() {
				                    public void setValues(PreparedStatement ps, Map<String, Integer> row) throws SQLException {
				                        ps.setInt(1, row.get("id"));
				                    }
				            });
							long endCronjobDetails = System.currentTimeMillis();
							log.info("Cronjobs data : Purged cronjobs details. Took:"+(endCronjobDetails-startCronjobDetails)+" millis");
							
							// deleting cronjobs table
							long startCronjob = System.currentTimeMillis();
							jdbcTemplate.batchUpdate(
							 	"DELETE FROM cronjobs WHERE id=?",
							 	cronjobsIdsToPurge,
							 	1024,
				                new ParameterizedPreparedStatementSetter<Map<String, Integer>>() {
				                    public void setValues(PreparedStatement ps, Map<String, Integer> row) throws SQLException {
				                        ps.setInt(1, row.get("id"));
				                    }
				            });
							long endCronjob = System.currentTimeMillis();
							log.info("Cronjobs data : Purged cronjobs. Took:"+(endCronjob-startCronjob)+" millis");
							
							// deleting cronjobs_transactions table
							long startCronjobTransaction = System.currentTimeMillis();
							jdbcTemplate.batchUpdate(
							 	"DELETE FROM cronjobs_transactions WHERE id=?",
							 	cronjobsIdsToPurge,
							 	1024,
				                new ParameterizedPreparedStatementSetter<Map<String, Integer>>() {
				                    public void setValues(PreparedStatement ps, Map<String, Integer> row) throws SQLException {
				                        ps.setInt(1, row.get("transaction_id"));
				                    }
				            });
							long endCronjobTransaction = System.currentTimeMillis();
							log.info("Cronjobs data : Purged cronjobs transactions. Took:"+(endCronjobTransaction-startCronjobTransaction)+" millis");
							long end = System.currentTimeMillis();
							log.info("Cronjobs data : All data was purged. Took:"+(end-start)+" millis");
						}
					}
					else {
						log.info("Cronjobs data: purge is disabled");
					}
					if(monthsPurgeMacrosTracking != null && monthsPurgeMacrosTracking.intValue()>0) {
						int monthToKeep = monthsPurgeMacrosTracking.intValue();
						String countQuery = "SELECT count(id) as nb FROM macros_tracking WHERE creation_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String deleteQuery  = "DELETE FROM macros_tracking WHERE creation_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String labelData = "Macros trackings";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
					}
					else {
						log.info("Macros trackings: purge is disabled");
					}
					
					if(monthsPurgeRatings != null && monthsPurgeRatings.intValue()>0) {
						int monthToKeep = monthsPurgeRatings.intValue();
						String countQuery = "SELECT count(id) as nb FROM ratings WHERE creation_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String deleteQuery  = "DELETE FROM ratings WHERE creation_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String labelData = "Users ratings";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
					}
					else {
						log.info("Users ratings: purge is disabled");
					}
					
					if(monthsPurgeTickets != null && monthsPurgeTickets.intValue()>0) {
						int monthToKeep = monthsPurgeTickets.intValue();
						String countQuery = "SELECT count(id) as nb FROM user_requests WHERE update_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String deleteQuery  = "DELETE FROM user_requests WHERE  update_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String labelData = "Users tickets";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
					}
					else {
						log.info("Users tickets: purge is disabled");
					}
					
					
					if(monthsPurgeMsTeams != null && monthsPurgeMsTeams.intValue()>0) {
						int monthToKeep = monthsPurgeMsTeams.intValue();
						
						String countQuery = "SELECT count(*) as nb FROM msteams_call_records_attendees WHERE creation_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String deleteQuery  = "DELETE FROM msteams_call_records_attendees WHERE creation_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						String labelData = "Call records participants";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
						
						countQuery = "SELECT count(*) as nb FROM msteams_call_records WHERE start_time<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM msteams_call_records WHERE start_time<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Call records";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
						
						countQuery = "SELECT count(*) as nb FROM msteams_call_records_modalities WHERE creation_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM msteams_call_records_modalities WHERE creation_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Call records modalities";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
												
						countQuery = "SELECT count(*) as nb FROM msteams_users_activity_report_for_day WHERE dateOfReport<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM msteams_users_activity_report_for_day WHERE dateOfReport<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Users activity by day";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
						
						
						countQuery = "SELECT count(*) as nb FROM msteams_users_snapshot_daily_by_connector WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM msteams_users_snapshot_daily_by_connector WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Users activity daily sanpshots";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
						
						countQuery = "SELECT count(*) as nb FROM msteams_users_snapshot_daily_by_segments WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM msteams_users_snapshot_daily_by_segments WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Users activity daily sanpshots by segments";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
						
					}
					else {
						log.info("MS Teams: purge is disabled");
					} 
					
					if(monthsPurgeJabber != null && monthsPurgeJabber.intValue()>0) {
						int monthToKeep = monthsPurgeJabber.intValue();
						
						String  countQuery = "SELECT count(*) as nb FROM jabber_call_domains WHERE 	call_id IN (SELECT call_id FROM jabber_cdr_data WHERE dateTimeOrigination<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH))";
						String deleteQuery  = "DELETE FROM jabber_call_domains WHERE call_id IN (SELECT call_id FROM jabber_cdr_data WHERE dateTimeOrigination<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH))";
						String labelData = "Jabber call domains";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
					
						countQuery = "SELECT count(*) as nb FROM jabber_cdr_data WHERE dateTimeOrigination<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM jabber_cdr_data WHERE dateTimeOrigination<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Jabber CDR data";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
						
						countQuery = "SELECT count(*) as nb FROM jabber_cmr_data WHERE dateTimeStamp<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM jabber_cmr_data WHERE dateTimeStamp<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Jabber CMR data";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
						
						countQuery = "SELECT count(*) as nb FROM jabber_snapshot_daily WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM jabber_snapshot_daily WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Jabber Snapshot Daily";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);

						countQuery = "SELECT count(*) as nb FROM jabber_snapshot_daily_by_domain WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						deleteQuery  = "DELETE FROM jabber_snapshot_daily_by_domain WHERE snap_date<DATE_ADD(current_date, INTERVAL -"+monthToKeep+" MONTH)";
						labelData = "Jabber Snapshot Daily by Domain";
						executePurge(countQuery, deleteQuery, labelData, monthToKeep);
	
					}
					else {
						log.info("Jabber: purge is disabled");
					}
					
					
					String sql = "UPDATE cronjobs SET payload=null WHERE payload IS NOT NULL AND creation_date<DATE_ADD(NOW(), INTERVAL -10 DAY) AND cronjobs.id NOT IN (SELECT DISTINCT job_id FROM cronjobs_details WHERE cronjobs_details.status = 0 AND cronjobs_details.nb_retry<3)";
					long startDelete = System.currentTimeMillis();
					jdbcTemplate.update(sql);
					long endDelete = System.currentTimeMillis();
					log.info("cronjobs.payload: purge terminated. Took:"+(endDelete-startDelete)+" millis");
					
					sql = "UPDATE cronjobs_details SET payload_result=null WHERE payload_result IS NOT NULL AND cronjobs_details.status=1 AND status_date IS NOT NULL AND status_date<DATE_ADD(NOW(), INTERVAL -10 DAY)";
					startDelete = System.currentTimeMillis();
					jdbcTemplate.update(sql);
					endDelete = System.currentTimeMillis();
					log.info("cronjobs_details.payload_result: purge terminated. Took:"+(endDelete-startDelete)+" millis");

					purgeInactiveUsers();
					
				}
				return RepeatStatus.FINISHED;
			}
		};

		Step step = new StepBuilder("PurgeDataStep", jobRepository).tasklet(takslet, transactionManager).build();
		return step;
	}
	
	
	private void purgeInactiveUsers() {
		Integer unpublishUnloggedUsersInDays = null;
		Integer deleteUnpublishedUsersInDays = null;
		try {
			Map<String, Object> config = jdbcTemplate.queryForMap(
					"SELECT unpublish_not_logged_users_after_days, delete_unpublished_users_after_days FROM configuration WHERE id=1");
			unpublishUnloggedUsersInDays = positiveDays(config.get("unpublish_not_logged_users_after_days"));
			deleteUnpublishedUsersInDays = positiveDays(config.get("delete_unpublished_users_after_days"));
		} catch (Exception e) {
			log.error("Unexpected exception", e);
			return;
		}

		if (unpublishUnloggedUsersInDays != null) {
			long start = System.currentTimeMillis();
			int updated = jdbcTemplate.update(
					"UPDATE users SET published=0 WHERE (last_login IS NOT NULL AND DATEDIFF(now(), last_login)>?) OR last_login IS NULL",
					unpublishUnloggedUsersInDays);
			long end = System.currentTimeMillis();
			log.info("Unpublish unlogged users since " + unpublishUnloggedUsersInDays + " day(s). Updated: " + updated + ". Took:" + (end - start) + " millis");
		} else {
			log.info("Unpublish unlogged users: purge is disabled");
		}

		if (deleteUnpublishedUsersInDays != null) {
			long start = System.currentTimeMillis();
			List<Integer> userIds = jdbcTemplate.query(
					"SELECT id FROM users WHERE published=0 AND last_login IS NOT NULL AND DATEDIFF(now(), last_login)>?",
					new RowMapper<Integer>() {
						@Override
						public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
							return rs.getInt("id");
						}
					},
					deleteUnpublishedUsersInDays);
			int found = userIds != null ? userIds.size() : 0;
			long endSelection = System.currentTimeMillis();
			log.info("Delete unpublished users since " + deleteUnpublishedUsersInDays + " day(s). Found: " + found + " users to delete. Selection took:" + (endSelection - start) + " millis");
			if (found > 0) {
				long startDelete = System.currentTimeMillis();
				deleteUsers(userIds);
				long endDelete = System.currentTimeMillis();
				log.info("Delete unpublished users: purge terminated. Took:" + (endDelete - startDelete) + " millis");
			}
		} else {
			log.info("Delete unpublished users: purge is disabled");
		}
	}

	private void deleteUsers(List<Integer> userIds) {
		deleteByIds("DELETE FROM users_rights WHERE user_id=?", userIds);
		deleteByIds("DELETE FROM users_webexsites WHERE user_id=?", userIds);
		deleteByIds("DELETE FROM users_msteams_tenants WHERE user_id=?", userIds);
		deleteByIds("DELETE FROM users_segments WHERE user_id=?", userIds);
		deleteByIds("DELETE FROM users_passwords WHERE user_id=?", userIds);
		deleteByIds("DELETE FROM users WHERE id=?", userIds);
	}

	private void deleteByIds(String sql, List<Integer> ids) {
		jdbcTemplate.batchUpdate(sql, ids, 1024, new ParameterizedPreparedStatementSetter<Integer>() {
			public void setValues(PreparedStatement ps, Integer id) throws SQLException {
				ps.setInt(1, id);
			}
		});
	}

	private Integer positiveDays(Object value) {
		if (value == null) {
			return null;
		}
		int days;
		if (value instanceof Number) {
			days = ((Number) value).intValue();
		} else {
			try {
				days = Integer.parseInt(value.toString().trim());
			} catch (Exception e) {
				log.error("Unexpected exception", e);
				return null;
			}
		}
		return days > 0 ? Integer.valueOf(days) : null;
	}

	private void executePurge(String countQuery, String deleteQuery, String labelData, int monthToKeep) {
		long start = System.currentTimeMillis();
		Integer nbElementsToPurge = jdbcTemplate.queryForObject(countQuery, Integer.class);
		long end = System.currentTimeMillis();
		log.info(labelData+": keep only "+monthToKeep+" month(s). Found: "+ (nbElementsToPurge != null ? nbElementsToPurge.intValue() : 0) + " records to delete. Selection took:"+(end-start)+" millis");
		if(nbElementsToPurge != null && nbElementsToPurge.intValue()>0) {
			long startDelete = System.currentTimeMillis();
			
			jdbcTemplate.update(deleteQuery);
			long endDelete = System.currentTimeMillis();
			log.info(labelData+": purge terminated. Took:"+(endDelete-startDelete)+" millis");
		}
	}
	
}
