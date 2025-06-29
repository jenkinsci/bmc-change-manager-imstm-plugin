package com.bmc.ims;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import groovy.lang.Binding;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext;
import org.kohsuke.stapler.verb.POST;

@Extension // annotation is required when writing a pipeline compatible plugin
public class BmcDlpBuilder extends Builder implements SimpleBuildStep, Serializable {
	private String server, port, user, jclContent, jclType, dlist, jobCard ,acctno, dPds, dOptionsPds, target, mode,
			ims_cmd, title, notes, vds, goodRC;
	private boolean dependent,coord,mark;
/*
 * Added transient To avoid SE_BAD_FIELD errors: defines non-transient non-serializable instance field
 */
	//private transient List<DlistRecord> dlistRecords = new ArrayList<DlistRecord>();
	//private transient List<DlpLoadLib> dlpLoadLibs = new ArrayList<DlpLoadLib>();
	
	private  List<DlistRecord> dlistRecords = new ArrayList<DlistRecord>();
	private  List<DlpLoadLib> dlpLoadLibs = new ArrayList<DlpLoadLib>();

	private  JCLService zosmf = null;

	private String groovyScript;
	private SecureGroovyScript script;

	private Secret pswd;
	//private static final long serialVersionUID = 1;

	// to avoid compilation error: annotated classes must have a public no-argument
	// // constructor
	public BmcDlpBuilder() {

	}
	//To customize serialization and deserialization, define readObject() and writeObject() methods in this class.
	// Throws exception while saving configuration 
	/*
	private void writeObject(java.io.ObjectOutputStream stream) throws java.io.IOException {
	    throw new java.io.NotSerializableException( getClass().getName() );
	}

	private void readObject(java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException {
	    throw new java.io.NotSerializableException( getClass().getName() );
	}
	*/
	/*
	 * Bind the Java attributes to the Jelly properties by: annotating our public
	 * constructor with @DataBoundConstructor and adding them to the constructor and
	 * providing a public getter method for each of them (be careful to name them
	 * accordingly- Fields in config.jelly must match the parameter names in the
	 * "DataBoundConstructor")
	 */
	@DataBoundConstructor
	public BmcDlpBuilder(String server, String port, String user, String pswd, String jclContent, String jclType,
			String dlist, String jobCard, String acctno, String dPds, String dOptionsPds, String target, String mode, String ims_cmd,
			String title, String notes, List<DlistRecord> dlistRecords, List<DlpLoadLib> dlpLoadLibs, String vds,
			String goodRC, boolean dependent , boolean mark, boolean coord) {

		this.dlistRecords = dlistRecords;
		this.dlpLoadLibs = dlpLoadLibs;

		this.server = server;
		this.port = port;
		this.user = user;
		this.pswd = Secret.fromString(pswd);
		this.jclContent = jclContent;
		this.jclType = jclType;
		this.dlist = dlist;
		this.jobCard = jobCard;
		this.acctno=acctno;
		this.dPds = dPds;
		this.target = target;
		this.mode = mode;
		this.ims_cmd = ims_cmd;
		this.dOptionsPds = dOptionsPds;
		this.title = title;
		this.notes = notes;
		this.vds = vds;
		this.goodRC = goodRC;

		this.dependent = dependent;
		this.mark=mark;
		this.coord=coord;

	}

	/*
	 * Getters and Setters !!! important for Configure to be able to read from
	 * config.xml
	 */

	public List<DlistRecord> getDlistRecords() {
		return this.dlistRecords;
	}

	public void setDlistRecords(List<DlistRecord> dlistRecords) {
		this.dlistRecords = dlistRecords;
	}

	public List<DlpLoadLib> getDlpLoadLibs() {
		return this.dlpLoadLibs;
	}

	public void setDlpLoadLibs(List<DlpLoadLib> dlpLoadLibs) {
		this.dlpLoadLibs = dlpLoadLibs;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}
	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}


	public Secret getPswd() {
		return pswd;
	}

	public void setPswd(Secret pswd) {
		this.pswd = pswd;
	}

	public void setJclContent(String jclContent) {
		this.jclContent = jclContent;
	}

	public String getJclContent() {
		return jclContent;
	}
	
	public String getJclType() {
		return jclType;
	}

	public void setJclType(String jclType) {
		this.jclType = jclType;
	}
	
	public String getDlist() {
		return dlist;
	}

	public void setDlist(String dlist) {
		this.dlist = dlist;
	}

	public String getJobCard() {
		return jobCard;
	}

	public void setJobCard(String jobCard) {
		this.jobCard = jobCard;
	}
	public String getAcctno() {
		return acctno;
	}
	public void setAcctno(String acctno) {
		this.acctno = acctno;
	}
	public String getdPds() {
		return dPds;
	}
	
	public void setdPds(String dPds) {
		this.dPds = dPds;
	}

	public String getdOptionsPds() {
		return dOptionsPds;
	}

	public void setdOptionsPds(String dOptionsPds) {
		this.dOptionsPds = dOptionsPds;
	}

	public String getTarget() {
		return target;
	}
	
	public void setTarget(String target) {
		this.target = target;
	}
	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getIms_cmd() {
		return ims_cmd;
	}

	public void setIms_cmd(String ims_cmd) {
		this.ims_cmd = ims_cmd;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
	public String getVds() {
		return vds;
	}

	public void setVds(String vds) {
		this.vds = vds;
	}

	public String getGoodRC() {
		return goodRC;
	}

	public void setGoodRC(String goodRC) {
		this.goodRC = goodRC;
	}

	
	public boolean isDependent() {
		return dependent;
	}

	public void setDependent(boolean dependent) {
		this.dependent = dependent;
	}

	public boolean isMark() {
		return mark;
	}

	public void setMark(boolean mark) {
		this.mark = mark;
	}
	public boolean isCoord() {
		return coord;
	}
	public void setCoord(boolean coord) {
		this.coord = coord;
	}

	private String inspectFailureInLogs(String log)
	{
		//System.out.println(log);
		// Set the job status according to RC
		// $HASP395 [job_name] ENDED - ABEND=S202
		// $HASP395 [job_name] ENDED - RC=0000

		if (log.contains("$HASP395"))
		{
			if (log.indexOf("RC") != -1)
			{
				String actRC=log.substring(log.indexOf("RC") + 3, log.indexOf("RC") + 7);
				//if RC!=0
				if (!actRC.equals("0000"))
				{
					int goodRc =Integer.parseInt(this.goodRC);
					int actualRc=Integer.parseInt(actRC);
					if(actualRc>goodRc)
						return "RC=" + actRC;
				}
			}
			else if (log.indexOf("ABEND") != -1)
				return "ABEND=" + log.substring(log.indexOf("ABEND") + 6, log.indexOf("ABEND") + 10);

		}
		// IEF453I [job_name] - JOB FAILED - JCL ERROR - TIME=02.12.30
		else if (log.contains("IEF453I"))
		{
			return "JOB FAILED - JCL ERROR";

		}
		// IEFC452I [job_name] - JOB NOT RUN - JCL ERROR
		else if (log.contains("IEFC452I"))
		{
			return "JOB NOT RUN - JCL ERROR";

		}
		//$HASP106 JOB DELETED BY JES2 OR CANCELLED BY OPERATOR BEFORE EXECUTION
		else if (log.contains("HASP106"))
		{
			return "JOB DELETED BY JES2 OR CANCELLED BY OPERATOR BEFORE EXECUTION ";

		}
		return null;
	}

	private String adjustBodyTo72Chars(String body)
	{
		String processedBody="";

		while(body.length()>0)
		{
			if(body.indexOf("\n")>73)
			{
				//break the line at the last " "
				for(int charsPerLine=72 ; charsPerLine>0 ; charsPerLine--)
				{
					String delimiter=body.substring(charsPerLine,charsPerLine+1);
					if(delimiter.equals(" ") || delimiter.equals(","))
					{
						processedBody=processedBody.concat(body.substring(0,charsPerLine)+"\n");
						body=body.substring(charsPerLine);
						break;
					}
				}//end for loop
			}
			// last line
			else if(body.indexOf("\n")==-1)
			{
				processedBody=processedBody.concat(body);
				body=body.substring(0,0); //sets length to 0
			}
			else if(body.indexOf("\n")<=73)
			{
				//String[] temp=body.split("\\n");
				processedBody=processedBody.concat(body.substring(0,body.indexOf("\n")+1));
				body=body.substring(body.indexOf("\n")+1);
			}
			
		}

		return processedBody;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see jenkins.tasks.SimpleBuildStep#perform(hudson.model.Run, hudson.FilePath,
	 * hudson.Launcher, hudson.model.TaskListener) For pipeline-compatible plugin
	 */
	@Override
	/*
	 * Deprecated
	 * https://javadoc.jenkins-ci.org/jenkins/tasks/SimpleBuildStep.html
	 */
	//public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
	//		throws InterruptedException, IOException {
	
	  public void perform(Run<?,?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)	
			  throws InterruptedException, IOException {
		ResponseObject resp = null;
		String url = "https://" + server + ":" + port + "/zosmf/restjobs/jobs";
		Properties headers = new Properties();
		String body = null;
		String jc=null; //jobcard
		String jobname = null;
		String jobid = null;
		String jobowner = null;
		String jobstatus = null;
		String jobtype = null;
		String jobretcode = null;
		long starttime = 0;
		long endtime = 0;
		long waittime = 0;
		String jobCompletion = null;
		String compareRC = "0001";

		/**************************************************************************/
		/* Login via zosmf */
		/**************************************************************************/
		listener.getLogger().println("user: " + user);
		listener.getLogger().println("server: " + server);
		listener.getLogger().println("port: " + port);

		this.zosmf = new JCLService(true);

		zosmf.login(server, port, user, pswd.getPlainText(), listener);

		/**************************************************************************/
		/* Submit jobs with z/OS jobs REST interface */
		/**************************************************************************/
		// ZOSMF job related manual
		// https://www.ibm.com/support/knowledgecenter/en/SSLTBW_2.1.0/com.ibm.zos.v2r1.izua700/IZUHPINFO_API_PutSubmitJob.htm

		/***************************/
		/* set request's body */
		/***************************/

		// Using Groovy script to do string interpolation and resolve placeholders
		// marked with ${}
		// share data between the java application and the groovy script using binding
		Binding binding = new Binding();
		//GroovyShell shell = new GroovyShell(binding);
		
		binding.setVariable("ACCTNO", this.acctno.toUpperCase());
		//jc=shell.evaluate("\"\"\"" + this.jobCard + "\"\"\"").toString();

		//Apply Groovy script security
		ClassLoader cl = getClass().getClassLoader();

		try
		{
			groovyScript="\"\"\"" + this.jobCard + "\"\"\"";
			script = new SecureGroovyScript(groovyScript, false, null).configuringWithKeyItem();
			jc=script.evaluate(cl, binding).toString();
			binding.setVariable("JOB_CARD", jc.toUpperCase());
		}
		catch (Exception e)
		{
			e.printStackTrace(listener.error("Failed to evaluate groovy script."));
		}


		binding.setVariable("VARLIST", this.vds.toUpperCase());

		List<DlistRecord> itemlist = getDlistRecords();

		int indx = 0;
		for (DlistRecord i : itemlist) {
			binding.setVariable("ACTION" + String.valueOf(indx), i.getAction().toUpperCase());
			binding.setVariable("ELEMENT_TYPE" + String.valueOf(indx), i.getElementType().toUpperCase());
			binding.setVariable("ELEMENT_NAME" + String.valueOf(indx), i.getElementName().toUpperCase());
			binding.setVariable("IMS_COMMAND" + String.valueOf(indx), i.getIms_cmd().toUpperCase());
			if (i.getElementType().equals("APPLCTN")) {
				binding.setVariable("NEW" + String.valueOf(indx), i.getAppnewname().toUpperCase());
				binding.setVariable("RES" + String.valueOf(indx), i.getAppres().toUpperCase());
				binding.setVariable("FP" + String.valueOf(indx), i.getAppfp().toUpperCase());
				binding.setVariable("TLS" + String.valueOf(indx), i.getApptls().toUpperCase());
				binding.setVariable("COPYACB" + String.valueOf(indx), i.getAppcopyacb().toUpperCase());
				binding.setVariable("RELGSAM" + String.valueOf(indx), i.getApprelgsam().toUpperCase());
			} else if (i.getElementType().equals("DATABASE")) {
				binding.setVariable("NEW" + String.valueOf(indx), i.getDbnewname().toUpperCase());
				binding.setVariable("RES" + String.valueOf(indx), i.getDbres().toUpperCase());
				binding.setVariable("COPYACB" + String.valueOf(indx), i.getDbcopyacb().toUpperCase());
				binding.setVariable("RELGSAM" + String.valueOf(indx), i.getDbrelgsam().toUpperCase());
			}
			else if (i.getElementType().equals("TRANSACT")) {
				binding.setVariable("NEW" + String.valueOf(indx), i.getTrannewname().toUpperCase());
				binding.setVariable("FP" + String.valueOf(indx), i.getTranfp().toUpperCase());
				binding.setVariable("INQ" + String.valueOf(indx), i.getTraninq().toUpperCase());
				binding.setVariable("TLS" + String.valueOf(indx), i.getTrantls().toUpperCase());
				
			}
			else if (i.getElementType().equals("TERMINAL")) {
				binding.setVariable("NEW" + String.valueOf(indx), i.getTerminalnewname().toUpperCase());
				binding.setVariable("MASK" + String.valueOf(indx), i.getTerminalmask().toUpperCase());
				
				
			}
			else if (i.getElementType().equals("LTERM")) {
				binding.setVariable("NEW" + String.valueOf(indx), i.getLtermnewname().toUpperCase());
				binding.setVariable("MASK" + String.valueOf(indx), i.getLtermmask().toUpperCase());
				
			}
			else if (i.getElementType().equals("SUBPOOL")) {
				binding.setVariable("NEW" + String.valueOf(indx), i.getSubpoolnewname().toUpperCase());
				binding.setVariable("MASK" + String.valueOf(indx), i.getSubpoolmask().toUpperCase());
				
			}
			else if (i.getElementType().equals("RTCODE")) {
				binding.setVariable("NEW" + String.valueOf(indx), i.getRtcnewname().toUpperCase());
				binding.setVariable("INQ" + String.valueOf(indx), i.getRtcinq().toUpperCase());
				
			}

			binding.setVariable("ACC" + String.valueOf(indx), i.getAcc().toUpperCase());
			binding.setVariable("AUTO" + String.valueOf(indx), i.getAuto().toUpperCase());
			binding.setVariable("RAND" + String.valueOf(indx), i.getRand().toUpperCase());
			binding.setVariable("RANDONLY" + String.valueOf(indx), i.getRandonly().toUpperCase());
			binding.setVariable("RLDAREAS" + String.valueOf(indx), i.getRldareas().toUpperCase());
			binding.setVariable("IOVFEXT" + String.valueOf(indx), i.getIovfext().toUpperCase());

			binding.setVariable("TY" + String.valueOf(indx), i.getTy().toUpperCase());
			binding.setVariable("SCHD" + String.valueOf(indx), i.getSchd().toUpperCase());
			
			binding.setVariable("DYN" + String.valueOf(indx), i.getDyn().toUpperCase());
			binding.setVariable("GPSB" + String.valueOf(indx), i.getGpsb().toUpperCase());
			binding.setVariable("LANG" + String.valueOf(indx), i.getLang().toUpperCase());
			
			
			binding.setVariable("PSB" + String.valueOf(indx), i.getPsb().toUpperCase());
			binding.setVariable("WFI" + String.valueOf(indx), i.getWfi().toUpperCase());
			binding.setVariable("NPRI" + String.valueOf(indx), i.getNpri().toUpperCase());
			binding.setVariable("LPRI" + String.valueOf(indx), i.getLpri().toUpperCase());
			binding.setVariable("LCO" + String.valueOf(indx), i.getLco().toUpperCase());
			binding.setVariable("MSEG" + String.valueOf(indx), i.getMseg().toUpperCase());
			binding.setVariable("RESP" + String.valueOf(indx), i.getResp().toUpperCase());
			binding.setVariable("CL" + String.valueOf(indx), i.getCl().toUpperCase());
			binding.setVariable("PLC" + String.valueOf(indx), i.getPlc().toUpperCase());
			binding.setVariable("TIME" + String.valueOf(indx), i.getTime().toUpperCase());
			binding.setVariable("PARA" + String.valueOf(indx), i.getPara().toUpperCase());			
			binding.setVariable("USCHD" + String.valueOf(indx), i.getUschd().toUpperCase());			
			binding.setVariable("RECV" + String.valueOf(indx), i.getRecv().toUpperCase());			
			binding.setVariable("EMHS" + String.valueOf(indx), i.getEmhs().toUpperCase());
			binding.setVariable("MPER" + String.valueOf(indx), i.getMper().toUpperCase());
			binding.setVariable("UC" + String.valueOf(indx), i.getUc().toUpperCase());
			binding.setVariable("EDIT" + String.valueOf(indx), i.getEdit().toUpperCase());
			binding.setVariable("LSID" + String.valueOf(indx), i.getLsid().toUpperCase());
			binding.setVariable("RSID" + String.valueOf(indx), i.getRsid().toUpperCase());
			binding.setVariable("SPA" + String.valueOf(indx), i.getSpa().toUpperCase());
			binding.setVariable("SPAD" + String.valueOf(indx), i.getSpad().toUpperCase());
			binding.setVariable("SEGS" + String.valueOf(indx), i.getSegs().toUpperCase());
			binding.setVariable("OSEG" + String.valueOf(indx), i.getOseg().toUpperCase());
			binding.setVariable("MSC" + String.valueOf(indx), i.getMsc().toUpperCase());
			binding.setVariable("DC" + String.valueOf(indx), i.getDc().toUpperCase());
			binding.setVariable("MREG" + String.valueOf(indx), i.getMreg().toUpperCase());
			binding.setVariable("SER" + String.valueOf(indx), i.getSer().toUpperCase());
			binding.setVariable("AOI" + String.valueOf(indx), i.getAoi().toUpperCase());			
			binding.setVariable("EXPTM" + String.valueOf(indx), i.getExptm().toUpperCase());
			
			
			
			binding.setVariable("SIGN" + String.valueOf(indx), i.getSign().toUpperCase());
			binding.setVariable("ASS" + String.valueOf(indx), i.getAss().toUpperCase());
			binding.setVariable("MSN" + String.valueOf(indx), i.getMsn().toUpperCase());
			binding.setVariable("L61" + String.valueOf(indx), i.getL61().toUpperCase());
			binding.setVariable("MSEG" + String.valueOf(indx), i.getMseg().toUpperCase());
			binding.setVariable("MSG" + String.valueOf(indx), i.getMsg().toUpperCase());
			
			binding.setVariable("NAME" + String.valueOf(indx), i.getRtcpsbname().toUpperCase());
			
			
			indx++;
		}

		List<DlpLoadLib> itemInLoadList = getDlpLoadLibs();
		int indx1 = 0;
		for (DlpLoadLib i : itemInLoadList) {
			binding.setVariable("DLP_LOAD" + String.valueOf(indx1), i.getLib().toUpperCase());
			indx1++;

		}

		binding.setVariable("DLP_OPTIONS_PDS", this.dOptionsPds.toUpperCase());
		binding.setVariable("DELTA_PDS", this.dPds.toUpperCase());
		binding.setVariable("DELTA_LIST_NAME", this.dlist.toUpperCase());
		binding.setVariable("DELTA_LIST_TITLE", this.title.toUpperCase());
		binding.setVariable("NOTES", this.notes.toUpperCase());
		binding.setVariable("TGT", this.target.toUpperCase());
		binding.setVariable("MOD", this.mode.toUpperCase());

		// enclosing between triple quotes/double-quotes to initialize the value of a
		// string with multiple lines
		//body = shell.evaluate("\"\"\"" + this.jclContent + "\"\"\"").toString().replace(",,","");

		 try
		 {
			 groovyScript="\"\"\"" + this.jclContent + "\"\"\"";
		     script = new SecureGroovyScript(groovyScript, false, null).configuring(ApprovalContext.create());
		     body=script.evaluate(cl, binding).toString().replace(",,","");
			 body=body.replace("(,", "(");
			 body=body.replace(",)", ")");
			 //This will replace every 72 characters with the same 80 characters and add a new line at the end
			 //body=body.replaceAll("(.{72})", "$1\n");

			 body=adjustBodyTo72Chars(body);

			 listener.getLogger().println("body:\n" + body);
		 }
		 catch (Exception e)
		 {
				e.printStackTrace(listener.error("Failed to evaluate groovy script."));
		}


		// listener.getLogger().println("body: " + body);

		/***************************/
		/* Set headers */
		/***************************/
		headers.put("Content-Type", "text/plain");
		headers.put("X-IBM-Intrdr-Class", "A");
		headers.put("X-IBM-Intrdr-Recfm", "F");
		headers.put("X-IBM-Intrdr-Lrecl", "80");
		headers.put("X-IBM-Intrdr-Mode", "TEXT");

		// submit
		resp = zosmf.doRequest(url, "PUT", body, headers, listener);
		listener.getLogger().println("Server returned response code: " + resp.status + " " + resp.jobId);

		if (resp.status >= 200 && resp.status <= 299) {

			jobname = resp.jobName;
			jobid = resp.jobId;
			listener.getLogger().println("Job " + jobid + " submitted successfully to " + server);
		}

		else {
			listener.getLogger().println("Error during job submission");
			run.setResult(Result.FAILURE);
			return;
		}

		/**************************************************************************/
		/* Obtain the job status after job submission */
		/**************************************************************************/
		boolean jobCompleted = false;
		int retcount = 1;

		try {
			url = "https://" + server + ":" + port + "/zosmf/restjobs/jobs/" + jobname + "/" + jobid;

			listener.getLogger().println("Waiting to retrieve job status...");

			headers = new Properties();
			headers.put("Content-Type", "application/json");
			body = "Obtain Job Status";
			
			while (!jobCompleted) {

				resp = zosmf.doRequest(url, "GET", body, headers, listener);
				//Bad request when the jobname includes # DLP#LIST
				//{"rc":4,"reason":7,"stack":"JesException: CATEGORY_SERVICE rc=4 reason=7 message=No match for method GET and pathInfo='\/DLP'\n\tat com.ibm.zoszmf.restjobs.util.JesException.serviceException(JesException.java:183)
				if(resp.status==400)	
				{
					run.setResult(Result.FAILURE);
					return;
				}	
				if (resp.jobStatus != null) {
					listener.getLogger()
							.println("Job Output Retrieval Attempt No= " + retcount + " status: " + resp.jobStatus);
					if (resp.jobStatus.equals("OUTPUT") || resp.jobStatus.equals("PRINT")) {
						jobCompleted = true;
					}
				}
				retcount++;

			}
		} catch (Exception ex) {
			ex.printStackTrace();
			listener.getLogger().println("Job status could not be retrieved");
			listener.getLogger().println(ex);

		}
		/**************************************************************************/
		/* List the job spool files of submitted job */
		/**************************************************************************/
		/*
		 * Sample response: [
		 * 
		 * {"jobid":JOB00023,"jobname":"TESTJOB1",subsystem:null,"id":1,
		 * "stepname":"JESE",procstep:null,"class":"H",
		 * "ddname":"JESMSGLG",record-count:14,"byte-count":1200, "records-url":
		 * "https:\/\/host:port\/zosmf\/restjobs\/jobs\/TESTJOB1\/JOB00023\/1/records"},
		 * {"jobid":JOB00023,"jobname":"TESTJOB1",subsystem:null,"id":2,
		 * "stepname":"JESE",procstep:null,"class":"H",
		 * "ddname":"JESJCL",record-count:10,"byte-count":526, "records-url":
		 * "https:\/\/host:port\/zosmf\/restjobs\/jobs\/TESTJOB1\/JOB00023\/2/records"},
		 * {"jobid":JOB00023,"jobname":"TESTJOB1",subsystem:null,"id":3,
		 * "stepname":"JESE",procstep:null,"class":"H",
		 * "ddname":"JESYSMSG",record-count:14,"byte-count":1255, "records-url":
		 * "https:\/\/host:port\/zosmf\/restjobs\/jobs\/TESTJOB1\/JOB00023\/3/records"},
		 * {"jobid":JOB00023,"jobname":"TESTJOB1",subsystem:null,"id":4,
		 * "stepname":"STEP57","procstep":"COMPILE","class":"H",
		 * "ddname":"SYSUT1","record-count":6,"byte-count":741, "records-url":
		 * "https:\/\/host:port\/zosmf\/restjobs\/jobs\/TESTJOB1\/JOB00023\/4/records"},
		 * {"jobid":JOB00023,"jobname":"TESTJOB1",subsystem:null,"id":5,
		 * "stepname":"STEP57","procstep":"COMPILE","class":"A",
		 * "ddname":"SYSPRINT","record-count":3,"byte-count":209, "records-url":
		 * "https:\/\/host:port\/zosmf\/restjobs\/jobs\/TESTJOB1\/JOB00023\/5/records"}
		 * ]
		 */

		try {
			url = "https://" + server + ":" + port + "/zosmf/restjobs/jobs/" + jobname + "/" + jobid + "/" + "files";

			// if (debug) {
			listener.getLogger().println("HTTPS URL path to list the spool files: " + url);
			// }

			headers = new Properties();
			headers.put("Content-Type", "application/json");
			// if (debug) {
			listener.getLogger().println("Waiting to retrieve list of job spool files...");
			// }
			body = "List spool files";
			resp = zosmf.doRequest(url, "GET", body, headers, listener);

			listener.getLogger().println("Server returned response code: " + resp.status);
			String message = "Additional diagnostic response messages:\n" + resp.statAndHeaders.toString();
			// if (debug) {
			listener.getLogger().println(message);
			// }

		} catch (Exception ex) {
			listener.getLogger().println("List of job spool files could not be retrieved");
			ex.printStackTrace();

			listener.getLogger().println(ex);

		}

		/**************************************************************************/
		/* Retrieve Log from spool files. Get the content of Job spool files */
		/**************************************************************************/

		StringBuffer append_data = new StringBuffer();
		String ACM_Security_temp = "";

		Writer w=null;
		PrintWriter pw=null;
		String logfilename = "";
		String logfileFolderPath="";
		String errormsg = null;

		try
		{
			int size = resp.idvalarr.size();
			ArrayList<String> idvalarr = new ArrayList<String>();
			ArrayList<String> ddnamevalarr = new ArrayList<String>();
			for (int i = 0; i < size; i++)
			{
				// if (debug) {
				listener.getLogger().println("ID number of the job spool files= " + resp.idvalarr.get(i));
				// }
				idvalarr.add(resp.idvalarr.get(i));
				ddnamevalarr.add(resp.ddnamevalarr.get(i));
			}
			// if (debug) {
			listener.getLogger().println("Before Job Log retrieval...");
			// }

			// issue requests per # of spool files
			for (int i = 0; i < size; i++)
				{
				url = "https://" + server + ":" + port + "/zosmf/restjobs/jobs/" + jobname + "/" + jobid + "/files"
						+ "/" + idvalarr.get(i) + "/records";

				// if (debug) {
				listener.getLogger().println("HTTPS URL path to retrieve content of spool files: " + url);
				// }

				headers = new Properties();
				headers.put("Content-Type", "plain/text");
				body = "Retrieve spool files content";

				resp = zosmf.doRequest(url, "GET", body, headers, listener);

				listener.getLogger().println(
						"Server returned response code for job spool file-" + idvalarr.get(i) + ": " + resp.status);
				String message = "Additional diagnostic response messages:\n" + resp.statAndHeaders.toString();
				// if (debug) {
				listener.getLogger().println(message);
				// }
				if (resp.ret_code == 8) {
					listener.getLogger().println(resp.resp_details);
					throw new Exception();
				}
				if (resp.status >= 200 && resp.status <= 299)
				{


					// write log file
					/*
					 * To avoid DM_DEFAULT_ENCODING error: use OutputStreamWriter instead of FileWriter
					 */
					/*
					FileWriter fw = null;
					BufferedWriter bw = null;
					*/
					// if (debug) {

					listener.getLogger().println("Writing the Job Log to workspace");
					// }
					//logfilename = jobname + "-" + jobid + "-" +i;
					logfilename =ddnamevalarr.get(i);
					//logfileFolderPath=jobname + "-" + jobid;
					logfileFolderPath=String.valueOf(run.getNumber());
					/*
					fw = new FileWriter(workspace + File.separator + logfilename);
					bw = new BufferedWriter(fw);
					bw.write(append_data.toString());
					File file = new File(someFilePath);
					*/
					File logfileFolder = new File(workspace + File.separator +logfileFolderPath);
					if (!logfileFolder.exists())
					{
						if (logfileFolder.mkdirs()) {
							System.out.println("directory " + logfileFolder+ " created");
						}

					}
					w = new OutputStreamWriter(new FileOutputStream(workspace + File.separator + logfileFolderPath + File.separator+ logfilename), "UTF-8");
					pw = new PrintWriter(w);
					String inputLine;
					BufferedReader in = null;
					/*
					 * Since the data to be processed, that is sent over the network might be very large, say more than a few hundred MB,
					 * switching to stream processing instead of StringBuffer usage, which loads all into memory
					 * and might cause OutOfMemoryError: Java heap space exceptions
					 */
					in = new BufferedReader(new InputStreamReader(resp.istream, "utf-8"));
					while ((inputLine = in.readLine()) != null)
					{
						pw.println(inputLine);
						errormsg=inspectFailureInLogs(inputLine);
						if(errormsg!=null)
						{

							run.setResult(Result.FAILURE);							
							break;
						}

					}
					in.close();
					resp.istream.close();
					pw.close();

					// bw.close();
					// fw.close();

					listener.getLogger().println("Spool file #"+i+" was successfully written to workspace");
					listener.getLogger().println("Job Output Path= " + workspace + File.separator + logfilename);

				} else if (resp.status == 401) {
					// error_var1 = "Incorrect user ID or password, or both, or the client did not
					// authenticate to z/OSMF";
					// listener.getLogger().println(ACMConst.BMCAMA00082E);
					throw new Exception();
				} else if (resp.status == 503) {
					// listener.getLogger().println("Server error. Server is not available");
					// listener.getLogger().println(ACMConst.BMCAMA00083E);
					throw new Exception();
				} else {
					// listener.getLogger().println("Other error. Please check the response code");
					// listener.getLogger().println(ACMConst.BMCAMA00084E);
					throw new Exception();
				}
			}//end for
			// if (debug) {
			//listener.getLogger().println("Job log successfully placed in buffer...");
			// }
			if(errormsg!=null)
				listener.getLogger().println(errormsg);
		}


		 catch (IOException ioex) {
			listener.getLogger().println("Job log STARTS here...");
			listener.getLogger().println();
			listener.getLogger().println(append_data.toString());
			listener.getLogger().println();
			listener.getLogger().println("Retrieved job log ends here...");
			listener.getLogger().println("Job Output Path= " + workspace + File.separator + logfilename);
			// if (debug)
			listener.getLogger().println(ioex);
			// if (ioex.getMessage() != null)
			// listener.getLogger().println(ioex.getMessage());
			ioex.printStackTrace();

		}
		catch (Exception ex) {
			listener.getLogger().println("Error while retrieving the job log");
			// if (debug)
			listener.getLogger().println(ex);
			// if (ex.getMessage() != null)
			// listener.getLogger().println(ex.getMessage());
			ex.printStackTrace();
			run.setResult(Result.FAILURE);
		}

		// if (append_data.toString().contains("BMC4568") ||
		// append_data.toString().contains("BMC56388"))
		// listener.getLogger().println("ACM Security return codes: " +
		// ACM_Security_temp);
		/*
		 * if(jobCompletion.equals("bad")){ ACMGetCredential acmgetcr = new
		 * ACMGetCredential(debug); acmgetcr.delIntFile(listener, intFileName); //throw
		 * new AbortException("Job Return Code= " + jobRC); throw new
		 * AbortException(ACMConst.BMCAMA00076E + jobRC); }
		 */
		// }

	}// end of perform

	@Extension
	/*
	 * This class is basically used for handling configuration of your Plugin. When
	 * you click on �Configure� link on Jenkins it basically calls this method and
	 * loads the configured data
	 */
	// To make for a more attractive and mnemonic usage style, you can depend on
	// org.jenkins-ci.plugins:structs and add a @Symbol to your Descriptor, uniquely
	// identifying it among extensions of its kind
	@Symbol("BMC DevOps for BMC AMI Change Manager for IMS TM Plugin")
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		private int lastEditorId = 0;
		//private List<DlistOperand> dlistOperands = new ArrayList<DlistOperand>();
		// @Rule
		// public JenkinsRule j = new JenkinsRule();
		//private Jenkins jn = Jenkins.get();
		
		// StaplerRequest req = Stapler.getCurrentRequest();
		
		/**
		 * The default constructor.
		 */
		public DescriptorImpl() {
			super(BmcDlpBuilder.class);
			load();
			
/*
			// DB
			dlistOperands.add(new DlistOperand("NEW", "dbnewname", "textbox", null,""));
			dlistOperands.add(new DlistOperand("RES", "dbres", "select", new String[] { "Y", "N" },""));
			dlistOperands.add(new DlistOperand("ACC", "acc", "select", new String[] { "UP", "EX", "RD", "RO" },""));
			dlistOperands.add(new DlistOperand("AUTO", "auto", "select", new String[] { "Y", "N" },""));
			dlistOperands.add(new DlistOperand("RAND", "rand", "select", new String[] { "Y", "N" },""));
			dlistOperands.add(new DlistOperand("RANDONLY", "randonly", "select", new String[] { "Y", "N" },""));
			dlistOperands.add(new DlistOperand("RLDAREAS", "rldareas", "select", new String[] { "Y", "N" },""));
			dlistOperands.add(new DlistOperand("IOVFEXT", "iovfext", "select", new String[] { "Y", "N" },""));

			// APPL
			dlistOperands.add(new DlistOperand("NEW", "appnewname", "textbox", null,""));
			dlistOperands.add(new DlistOperand("RES", "appres", "select", new String[] { "Y", "N" },""));
			dlistOperands.add(new DlistOperand("TY", "ty", "select", new String[] { "TP", "BATCH", "MPP", "BMP" },""));
			dlistOperands.add(new DlistOperand("SCHD", "schd", "select", new String[] { "SERIAL", "PARALLEL" },""));
			dlistOperands.add(new DlistOperand("FP", "appfp", "select", new String[] { "Y", "N" },""));
			dlistOperands.add(new DlistOperand("DYN", "dyn", "select", new String[] { "Y", "N" },""));
			dlistOperands.add(new DlistOperand("GPSB", "gpsb", "select", new String[] { "Y", "N" },""));
			dlistOperands.add(new DlistOperand("LANG", "lang", "select",
					new String[] { "COBOL", "ASSEM", "PASCAL", "PL/I", "JAVA", "NONE" },""));
			dlistOperands.add(new DlistOperand("TLS", "apptls", "select", new String[] { "Y", "N" },""));

			// RTCODE
			dlistOperands.add(new DlistOperand("NEW", "rtcnewname", "textbox", null,""));
			dlistOperands.add(new DlistOperand("INQ", "rtcsinq", "select", new String[] { "Y", "N" },""));
			
			//TRANSACT
			dlistOperands.add(new DlistOperand("PSB", "psb", "textbox", null,""));
			dlistOperands.add(new DlistOperand("NEW", "trannewname", "textbox", null,""));
			dlistOperands.add(new DlistOperand("WFI", "wfi", "select", new String[] {  "Y", "N" },""));
			dlistOperands.add(new DlistOperand("NPRI", "npri", "number", null,"1"));
			dlistOperands.add(new DlistOperand("LPRI", "lpri", "number", null,"1"));
			dlistOperands.add(new DlistOperand("LCO", "lco", "number", null,"65535"));
			dlistOperands.add(new DlistOperand("MSEG", "mseg", "select", new String[] { "Y", "N" },""));
			dlistOperands.add(new DlistOperand("RESP", "resp", "select",new String[] { "Y", "N" },""));
			dlistOperands.add(new DlistOperand("CL", "cl", "number", null,"1"));
			dlistOperands.add(new DlistOperand("PLC", "plc", "number", null,"65535"));
			dlistOperands.add(new DlistOperand("TIME", "time", "number", null,"65535"));
			dlistOperands.add(new DlistOperand("PARA", "para", "number", null,"0"));
			
			dlistOperands.add(new DlistOperand("USCHD", "uschd", "number", null,""));
			dlistOperands.add(new DlistOperand("INQ", "traninq", "select", new String[] {  "Y", "N" },""));
			dlistOperands.add(new DlistOperand("RECV", "recv", "select", new String[] {  "Y", "N" },""));
			dlistOperands.add(new DlistOperand("FP", "tranfp", "select", new String[] {  "Y", "N" },""));
			dlistOperands.add(new DlistOperand("EMHS", "emhs", "number", null,""));
			dlistOperands.add(new DlistOperand("MPER", "mper", "select", new String[] {  "SNGL", "MULT" },""));
			dlistOperands.add(new DlistOperand("UC", "uc", "select", new String[] { "Y", "N" },""));
			dlistOperands.add(new DlistOperand("EDIT", "edit", "textbox",null,""));
			dlistOperands.add(new DlistOperand("LSID", "lsid", "number", null,""));
			dlistOperands.add(new DlistOperand("RSID", "rsid", "number", null,""));
			dlistOperands.add(new DlistOperand("SPA", "spa", "number", null,""));
			dlistOperands.add(new DlistOperand("SPAD", "spad", "select", new String[] {  "STRUNC", "RTRUNC" },""));
			dlistOperands.add(new DlistOperand("SEGS", "segs", "number", null,""));
			dlistOperands.add(new DlistOperand("OSEG", "oseg", "number", null,""));
			dlistOperands.add(new DlistOperand("MSC", "msc", "select", new String[] {  "Y", "N" },""));
			dlistOperands.add(new DlistOperand("DC", "dc", "select", new String[] {  "Y", "N" },""));
			dlistOperands.add(new DlistOperand("MREG", "mreg", "number", null,""));
			dlistOperands.add(new DlistOperand("SER", "ser", "select", new String[] { "Y", "N" },""));
			dlistOperands.add(new DlistOperand("AOI", "aoi", "select", new String[] { "N", "Y","T","C" },""));
			dlistOperands.add(new DlistOperand("TLS", "trantls", "select",new String[] { "Y", "N" },""));
			dlistOperands.add(new DlistOperand("EXPTM", "exptm", "number", null,""));
			
			// TERMINAL
			dlistOperands.add(new DlistOperand("MASK", "terminalmask", "textbox", null,""));
			dlistOperands.add(new DlistOperand("NEW", "terminalnewname", "textbox", null,""));
			dlistOperands.add(new DlistOperand("SIGN", "sign", "select", new String[] {  "Y", "N" },""));
			
			//LTERM
			dlistOperands.add(new DlistOperand("MASK", "ltermmask", "textbox", null,""));
			dlistOperands.add(new DlistOperand("NEW", "ltermnewname", "textbox", null,""));
			dlistOperands.add(new DlistOperand("ASS", "ass", "textbox", null,""));
			dlistOperands.add(new DlistOperand("MSN", "msn", "textbox", null,""));
			dlistOperands.add(new DlistOperand("L61", "l61", "select", new String[] {  "Y", "N" },""));
			
			//SUBPOOL
			dlistOperands.add(new DlistOperand("MASK", "subpoolmask", "textbox", null,""));
			dlistOperands.add(new DlistOperand("NEW", "subpoolnewname", "textbox", null,""));
			dlistOperands.add(new DlistOperand("MSG", "msg", "select", new String[] {  "SYSINFO", "NONIOPCB" },""));
	*/		
		}

		
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public String getDisplayName() {
			// TODO Auto-generated method stub
			return "BMC AMI DevOps for Change Manager for IMS TM";

		}

		@JavaScriptMethod
		public synchronized String createUniqueId() {
			// System.out.println("marit"+Jenkins.getInstanceOrNull().getJobNames());
			return String.valueOf(lastEditorId++);

		}
		/*
		@JavaScriptMethod
		public synchronized void issueError() throws FormValidation{
			//	if (value != "" && value!=null)
			//		return FormValidation.ok();
			//	else
					// return FormValidation.CheckMethod.this.;
					throw FormValidation.warning("Marit");
			}
		*/	
/*
		@JavaScriptMethod
		public synchronized boolean reloadJobConfig() throws Exception {

			// System.out.println(jn);
			// jn.doConfigSubmit(Stapler.getCurrentRequest(),Stapler.getCurrentResponse());
			// jn.reload();

			// XmlPage page = getRssAllAtomPage();
			// NodeList allLinks = page.getXmlDocument().getElementsByTagName("link");
			// System.out.println(allLinks);

			return true;

		}
*/
		/*
		 * private XmlPage getRssAllAtomPage() throws Exception { return (XmlPage)
		 * createWebClient().getPage(getConfigPage()); // descriptor. //
		 * submit(createWebClient().getPage(view, //
		 * "configure").getFormByName("viewConfig")); }
		 * 
		 * public WebClient createWebClient() { return new WebClient(); }
		 */
		@Override
		public String getId() {
			// TODO Auto-generated method stub
			return super.getId();
		}
/*
		public List<DlistOperand> getDlistOperands() {
			return dlistOperands;
		}
*/
		
		//Client side validation
		//triggered by either Configure or Add new Build Step or tab or selecting a different field
		//public FormValidation doCheckDlist(@QueryParameter String value) {
		//	if (value.trim() != "" && value!=null)
		//		return FormValidation.ok();
		//	else
				// return FormValidation.CheckMethod.this.;
		//		return FormValidation.warning("Delta list name is required!");
		//}

		@POST
		public FormValidation doCheckPswd(@QueryParameter String value)
		{
			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
				result = FormValidation.error("Password is required!");

			return result;
		}

		@POST
		public FormValidation doCheckServer(@QueryParameter String value)
		{
			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
				result = FormValidation.error("Required field!");

			return result;
		}

		@POST
		public FormValidation doCheckPort(@QueryParameter String value)
		{
			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
				result = FormValidation.error("Required field!");

			return result;
		}

		@POST
		public FormValidation doCheckUser(@QueryParameter String value)
		{
			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
				result = FormValidation.error("Required field!");

			return result;
		}

		@POST
		public FormValidation doCheckDPds(@QueryParameter String value)
		{
			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
				result = FormValidation.error("Required field!");

			return result;
		}

		@POST
		public FormValidation doCheckDlist(@QueryParameter String value)
		{
			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
				result = FormValidation.error("Required field!");

			return result;
		}

		@POST
		public FormValidation doCheckLib(@QueryParameter String value)
		{
			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
				result = FormValidation.error("Load library is rerquired!");

			return result;
		}
		@POST
		public FormValidation doCheckElementName(@QueryParameter String value)
		{
			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);
			if (tempValue.isEmpty())
				result = FormValidation.error("A name is required for this element!");

			else if(value.length()>8)
				result = FormValidation.warning("Element name should be 1-8 characters");

			return result;
		}


		@POST
		public FormValidation doCheckPsb(@QueryParameter String value , @QueryParameter String action)
		{
			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);
			if(action.contains("ADD") || action.contains("ADDREV"))
				if (tempValue.isEmpty())
					result = FormValidation.error("PSB name is required!");

			return result;
		}

		@POST
		public FormValidation doCheckAppres(@QueryParameter String value, @QueryParameter String dyn, @QueryParameter String gpsb) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if(value.equals("Y")) {
				if (dyn.equals("Y") || dyn.equals("DOPT"))
					result = FormValidation.warning("RES=Y and DYN=" + dyn + " are mutually exclusive");
				else if (gpsb.equals("Y"))
					result = FormValidation.warning("RES=Y and GPSB=" + gpsb + " are mutually exclusive");
			}
			return result;
		}
		@POST
		public FormValidation doCheckBmcRldAreas(@QueryParameter boolean value, @QueryParameter boolean  bmcAuto, @QueryParameter boolean  bmcRand,
												 @QueryParameter boolean  bmcRandOnly, @QueryParameter boolean bmcIoVfExt) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if(value==true && (bmcAuto==true || bmcRand==true || bmcRandOnly==true || bmcIoVfExt==true ) )
				result = FormValidation.error("If RLDAREAS is specified then no other field (except for RELGSAM and COPYACB) can be specified.");
			return result;
		}

		@POST
		public FormValidation doCheckBmcIoVfExt(@QueryParameter boolean value, @QueryParameter boolean  bmcAuto, @QueryParameter boolean  bmcRand,
												 @QueryParameter boolean  bmcRandOnly, @QueryParameter boolean bmcRldAreas) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if(value==true && (bmcAuto==true || bmcRand==true || bmcRandOnly==true || bmcRldAreas==true ) )
				result = FormValidation.error("If IOVFEXT is specified then no other field (except for RELGSAM and COPYACB) can be specified.");
			return result;
		}


		@POST
		public FormValidation doCheckTy(@QueryParameter String value, @QueryParameter String appfp) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if(( value.equals("BATCH") || value.equals("BMP")  )&& appfp.equals("Y"))
				result = FormValidation.warning("TY="+value +" and FP=" + appfp + " are mutually exclusive");

			return result;
		}

		@POST
		public FormValidation doCheckSchd(@QueryParameter String value, @QueryParameter String dyn) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if( value.equals("PARALLEL") && ( dyn.equals("Y")  || dyn.equals("DOPT")))
				result = FormValidation.warning("SCHD="+value +" and DYN=" + dyn + " are mutually exclusive");

			return result;
		}



		@POST
		public FormValidation doCheckAppfp(@QueryParameter String value, @QueryParameter boolean bmcLang,@QueryParameter String lang ,@QueryParameter String ty) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if(value.equals("Y"))
			{
				if(lang.equals("JAVA") && bmcLang==true)
					result=FormValidation.warning("FP="+ value+" and LANG="+lang+" are mutually exclusive");
				else if(ty.equals("BMP") || ty.equals("BATCH"))
					result=FormValidation.warning("FP="+ value+" and TY="+ty+" are mutually exclusive");
			}

			return result;
		}

		@POST
		public FormValidation doCheckDyn(@QueryParameter String value, @QueryParameter String gpsb ,@QueryParameter String schd, @QueryParameter String appres) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if(value.equals("Y") || value.equals("DOPT") )
			{
				if(gpsb.equals("Y"))
					result=FormValidation.warning("DYN="+ value+" and GPSB="+gpsb+" are mutually exclusive");
				else if(schd.equals("PARALLEL") )
					result=FormValidation.warning("DYN="+ value+" and SCHD="+schd+" are mutually exclusive");
				else if(appres.equals("Y") )
					result=FormValidation.warning("DYN="+ value+" and RES="+appres+" are mutually exclusive");

			}

			return result;
		}
		@POST
		public FormValidation doCheckGpsb(@QueryParameter String value, @QueryParameter boolean bmcLang, @QueryParameter String dyn, @QueryParameter String appres ) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if(value.equals("Y") ) {
				if(bmcLang==false )
					result = FormValidation.error("If GPSB=YES, LANG must be specified");
				if(dyn.equals("DOPT") || dyn.equals("Y"))
					result = FormValidation.warning("GPSB=" + value + " and DYN="+dyn+" are mutually exclusive");
				if(appres.equals("Y"))
					result = FormValidation.warning("GPSB=" + value + " and RES="+appres+" are mutually exclusive");
			}
			return result;
		}

		@POST
		public FormValidation doCheckLang(@QueryParameter String value, @QueryParameter boolean bmcLang, @QueryParameter String appfp, @QueryParameter String gpsb ) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if(bmcLang==true ) {
				if(gpsb.equals("N") )
					result = FormValidation.error("LANG is only valid if GPSB=YES");
				if(value.equals("JAVA") && appfp.equals("Y"))
					result = FormValidation.warning("LANG=" + value + " and FP="+appfp+" are mutually exclusive");

			}
			return result;
		}

		@POST
		public FormValidation doCheckBmcWfi(@QueryParameter String mper, @QueryParameter String wfi) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if(wfi.equals("Y") && mper.equals("MULT"))
				result=FormValidation.warning("WFI and MPER=MULT are mutually exclusive");

			return result;
		}

		@POST
		public FormValidation doCheckMseg(@QueryParameter String value, @QueryParameter String tranfp) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if(value.equals("Y") && tranfp.equals("Y"))
				result=FormValidation.warning("Fast Path transactions must have MSEG=N (The incoming message can't contain more than one segment)." );

			return result;
		}

		@POST
		public FormValidation doCheckResp(@QueryParameter String value, @QueryParameter String tranfp) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if( tranfp.equals("Y") && value.equals("N"))
				result=FormValidation.warning("Fast Path transactions must have RESP=Y" );

			return result;
		}

		@POST
		public FormValidation doCheckTraninq(@QueryParameter String value, @QueryParameter String recv) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if( value.equals("N") && recv.equals("N"))
				result=FormValidation.warning("INQ=N and RECV=N are mutually exclusive" );

			return result;
		}

		@POST
		public FormValidation doCheckRecv(@QueryParameter String value, @QueryParameter String traninq, @QueryParameter String tranfp) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if( value.equals("N"))
			{
				if( traninq.equals("N"))
					result=FormValidation.warning("RECV=N and INQ=N are mutually exclusive" );
				if( tranfp.equals("Y"))
					result=FormValidation.warning("RECV=N and FP=Y are mutually exclusive" );
			}
			return result;
		}

		@POST
		public FormValidation doCheckTranfp(@QueryParameter String value, @QueryParameter String mseg, @QueryParameter String recv, @QueryParameter String resp, @QueryParameter String spad, @QueryParameter boolean bmcSpad, @QueryParameter boolean bmcEmhs)
 {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if( value.equals("Y"))
			{
				if( mseg.equals("Y"))
					result=FormValidation.warning("FP=Y and MSEG=Y are mutually exclusive" );
				if( recv.equals("N"))
					result=FormValidation.warning("FP=Y and RECV=N are mutually exclusive" );
				if( resp.equals("N"))
					result=FormValidation.warning("FP=Y and RESP=N are mutually exclusive" );
				if( spad.equals("STRUNC") && bmcSpad==true)
					result=FormValidation.warning("FP=Y and SPAD=STRUNC are mutually exclusive" );
			}
			else if ( value.equals("N"))
				if (bmcEmhs==true)
					result=FormValidation.warning("FP(N) and EMHS>0 are mutually exclusive");
			return result;
		}

		@POST
		public FormValidation doCheckMper(@QueryParameter String value, @QueryParameter String wfi) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if(wfi.equals("Y") && value.equals("MULT"))
				result=FormValidation.warning("MPER=MULT and WFI=Y are mutually exclusive");

			return result;
		}

		@POST
		public FormValidation doCheckBmcRandOnly(@QueryParameter boolean value) {

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if(value==true)
				result=FormValidation.ok("If RANDONLY is specified then no other field can be specified.");

			return result;
		}

		@POST
		public FormValidation doCheckBmcspad(@QueryParameter boolean value, @QueryParameter String spad, @QueryParameter String tranfp)
		{

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			if( value==true)
			{
				if( tranfp.equals("Y") && spad.equals("STRUNC"))
					result=FormValidation.warning("FP=Y and SPAD=STRUNC are mutually exclusive" );
			}
			return result;
		}

		@POST
		public FormValidation doCheckSpad(@QueryParameter String value,  @QueryParameter String tranfp)
		{

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);


			if( tranfp.equals("Y") && value.equals("STRUNC"))
					result=FormValidation.warning("FP=Y and SPAD=STRUNC are mutually exclusive" );

			return result;
		}
		@POST
		public FormValidation doCheckEmhs(@QueryParameter String value, @QueryParameter boolean bmcEmhs, @QueryParameter String tranfp)
		{

			FormValidation result = null;

			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);

			if(bmcEmhs==true)
				if (tranfp.equals("N"))
					result = FormValidation.warning("EMHS>0 and FP(N) are mutually exclusive");
			else if (! tempValue.isEmpty())
				if(Integer.parseInt(value)<12 || Integer.parseInt(value)>30720)
					result = FormValidation.error("Valid range between 12-30,720");

			return result;

		}

		@POST
		public FormValidation doCheckNpri(@QueryParameter String value)
		{
			FormValidation result = null;
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);

			if (! tempValue.isEmpty())
				if(Integer.parseInt(value)<0 || Integer.parseInt(value)>14)
					result = FormValidation.error("Valid range between 0-14");

			return result;
		}


		@POST
		public FormValidation doCheckLpri(@QueryParameter String value)
		{
			FormValidation result = null;
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);

			if (! tempValue.isEmpty())
				if(Integer.parseInt(value)<0 || Integer.parseInt(value)>14)
					result = FormValidation.error("Valid range between 0-14");

			return result;
		}

		@POST
		public FormValidation doCheckLco(@QueryParameter String value)
		{
			FormValidation result = null;
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);

			if (! tempValue.isEmpty())
				if(Integer.parseInt(value)<1 || Integer.parseInt(value)>65535)
					result = FormValidation.error("Valid range between 1-65,535");

			return result;
		}

		@POST
		public FormValidation doCheckPlc(@QueryParameter String value)
		{
			FormValidation result = null;
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);

			if (! tempValue.isEmpty())
				if(Integer.parseInt(value)<0 || Integer.parseInt(value)>65535)
					result = FormValidation.error("Valid range between 0-65,535");

			return result;
		}

		@POST
		public FormValidation doCheckTime(@QueryParameter String value)
		{
			FormValidation result = null;
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);

			if (! tempValue.isEmpty())
				if(Integer.parseInt(value)<1 || Integer.parseInt(value)>65535)
					result = FormValidation.error("Valid range between 1- 65,535");

			return result;
		}

		@POST
		public FormValidation doCheckPara(@QueryParameter String value)
		{
			FormValidation result = null;
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);
			try {
				if (tempValue.toUpperCase().equals("NONE"))
					return result;
				else if (!tempValue.isEmpty())
					if (Integer.parseInt(value) < 0 || Integer.parseInt(value) > 32767)
						result = FormValidation.error("Valid range between 0-32,767");
			}
			catch(NumberFormatException ex)
			{
				result= FormValidation.error("Valid range between 0-32,767");
				return result;
			}
			return result;
		}
		@POST
		public FormValidation doCheckCl(@QueryParameter String value)
		{
			FormValidation result = null;
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);

			if (! tempValue.isEmpty())
				if(Integer.parseInt(value)<1 || Integer.parseInt(value)>999)
					result = FormValidation.error("Valid range between 1-999");

			return result;
		}
		@POST
		public FormValidation doCheckUschd(@QueryParameter String value)
		{
			FormValidation result = null;
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);

			if (! tempValue.isEmpty())
				if(Integer.parseInt(value)<1 || Integer.parseInt(value)>4)
					result = FormValidation.error("Valid range between 1-4");

			return result;
		}
		@POST
		public FormValidation doCheckSegs(@QueryParameter String value)
		{
			FormValidation result = null;
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);

			if (! tempValue.isEmpty())
				if(Integer.parseInt(value)<0 || Integer.parseInt(value)>65535)
					result = FormValidation.error("Valid range between 0-65,535");

			return result;
		}

		@POST
		public FormValidation doCheckOseg(@QueryParameter String value)
		{
			FormValidation result = null;
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);

			if (! tempValue.isEmpty())
				if(Integer.parseInt(value)<0 || Integer.parseInt(value)>65535)
					result = FormValidation.error("Valid range between 0-65,535");

			return result;
		}



		@POST
		public FormValidation doCheckLsid(@QueryParameter String value)
		{
			FormValidation result = null;
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);

			if (! tempValue.isEmpty())
				if(Integer.parseInt(value)<1 || Integer.parseInt(value)>2036)
					result = FormValidation.error("Valid range between 1-2036");

			return result;
		}

		@POST
		public FormValidation doCheckRsid(@QueryParameter String value)
		{
			FormValidation result = null;
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);

			if (! tempValue.isEmpty())
				if(Integer.parseInt(value)<1 || Integer.parseInt(value)>2036)
					result = FormValidation.error("Valid range between 1-2036");

			return result;
		}

		@POST
		public FormValidation doCheckSpa(@QueryParameter String value)
		{
			FormValidation result = null;
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);

			if (! tempValue.isEmpty())
				if(Integer.parseInt(value)<16 || Integer.parseInt(value)>32767)
					result = FormValidation.error("Valid range between 16-32,767");

			return result;
		}

		@POST
		public FormValidation doCheckMreg(@QueryParameter String value)
		{
			FormValidation result = null;
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);

			if (! tempValue.isEmpty())
				if(Integer.parseInt(value)<0 || Integer.parseInt(value)>255)
					result = FormValidation.error("Valid range between 0-255");

			return result;
		}

		@POST
		public FormValidation doCheckExptm(@QueryParameter String value)
		{
			FormValidation result = null;
			Jenkins.get().checkPermission(Jenkins.ADMINISTER);

			String tempValue = StringUtils.trimToEmpty(value);

			if (! tempValue.isEmpty())
				if(Integer.parseInt(value)<0 || Integer.parseInt(value)>65535)
					result = FormValidation.error("Valid range between 0-65,535");

			return result;
		}

		//doFill{fieldname}Items		
		
		public ListBoxModel doFillApprelgsamItems() {
		    ListBoxModel items = new ListBoxModel();		    
		    items.add( "Y", "Y" );		    		
		    items.add( "N", "N" );
		    return items;
		}  
		
		public ListBoxModel doFillDbrelgsamItems() {
		    ListBoxModel items = new ListBoxModel();		    
		    items.add( "Y", "Y" );		    		
		    items.add( "N", "N" );
		    return items;
		}  
		
		public ListBoxModel doFillAppcopyacbItems() {
		    ListBoxModel items = new ListBoxModel();		   
		    items.add( "Y", "Y" );		
		    items.add( "N", "N" );
		    return items;
		}  
		
		public ListBoxModel doFillDbcopyacbItems() {
			ListBoxModel items = new ListBoxModel();
		    items.add( "Y", "Y" );
		    items.add( "N", "N" );
		    return items;
		}  
		
		
		
		
		public ListBoxModel doFillJclTypeItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "CREATE A DELTA LIST", "crtDeltaList" );
		    items.add( "CHECK", "check" );
		    items.add( "EXEC", "exec" );
		   
		    return items;

		}
		
		public ListBoxModel doFillActionItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "ADD", "ADD" );
		    items.add( "REVISE", "REVISE" );
		    items.add( "ADDREV", "ADDREV" );
		    items.add( "DELETE", "DELETE" );
		    items.add( "RELOAD", "RELOAD" );
		    items.add( "EXECUTE", "EXECUTE" );
		   
		    return items;

		}
		
		public ListBoxModel doFillElementTypeItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "APPLCTN", "APPLCTN" );
		    items.add( "TRANSACT", "TRANSACT" );
		    items.add( "DATABASE", "DATABASE" );
		    items.add( "RTCODE", "RTCODE" );
		    items.add( "TERMINAL", "TERMINAL" );
		    items.add( "LTERM", "LTERM" );
		    items.add( "SUBPOOL", "SUBPOOL" );
		   
		    return items;

		}
		
		public ListBoxModel doFillModeItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "OPTIMIZE", "OPTIMIZE" );
		    items.add( "ELEMENT", "ELEMENT" );		  		   
		    return items;
		}   
		
		public ListBoxModel doFillDbresItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "N", "N" );
		    items.add( "Y", "Y" );		    		  		   
		    return items;
		}  
		
		public ListBoxModel doFillAccItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "EX", "EX" );		    
		    items.add( "RO", "RO" );
		    items.add( "RD", "RD" );	
		    items.add( "UP", "UP" );
		    return items;
		}  
		
		public ListBoxModel doFillAutoItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "N", "N" );
		    items.add( "Y", "Y" );		    		  		   
		    return items;
		}  
		public ListBoxModel doFillRandItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "N", "N" );
		    items.add( "Y", "Y" );		    		  		   
		    return items;
		}  
		public ListBoxModel doFillRandonlyItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "N", "N" );	
		    items.add( "Y", "Y" );		   	  		   
		    return items;
		}  
		public ListBoxModel doFillRldareasItems() {
		    ListBoxModel items = new ListBoxModel();		    
		    items.add( "N", "N" );	
		    items.add( "Y", "Y" );
		    return items;
		}  
		public ListBoxModel doFillIovfextItems() {
		    ListBoxModel items = new ListBoxModel();		   
		    items.add( "N", "N" );		  		   
		    items.add( "Y", "Y" );
		    return items;
		}  
		
		public ListBoxModel doFillTyItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "TP", "TP" );
		    items.add( "BATCH", "BATCH" );	
		    items.add( "MPP", "MPP" );	
		    items.add( "BMP", "BMP" );	
		    return items;
		} 
	
		public ListBoxModel doFillSchdItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "SERIAL", "SERIAL" );
		    items.add( "PARALLEL", "PARALLEL" );		  		   
		    return items;
		}  
	
		public ListBoxModel doFillAppfpItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "N", "N" );
		    items.add( "Y", "Y" );		  		   
		    return items;
		}  
		
		public ListBoxModel doFillDynItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "N", "N" );
		    items.add( "Y", "Y" );	
		    items.add( "DOPT", "DOPT" );	
		    return items;
		}  
		public ListBoxModel doFillGpsbItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "N", "N" );
		    items.add( "Y", "Y" );		  		   
		    return items;
		}  
		
		public ListBoxModel doFillLangItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "COBOL", "COBOL" );
		    items.add( "ASSEM", "ASSEM" );	
		    items.add( "PASCAL", "PASCAL" );	
		    items.add( "PL/I", "PL/I" );	
		    items.add( "JAVA", "JAVA" );	
		    items.add( "NONE", "NONE" );	
		    return items;
		}  
		public ListBoxModel doFillTlsItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "N", "N" );	
		    items.add( "Y", "Y" );		    	  		   
		    return items;
		} 
		
		
		
		public ListBoxModel doFillAppresItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "N", "N" );
		    items.add( "Y", "Y" );		  		   
		    return items;
		}
		public ListBoxModel doFillWfiItems() {
		    ListBoxModel items = new ListBoxModel();		   
		    items.add( "N", "N" );	
		    items.add( "Y", "Y" );
		    return items;
		}
		public ListBoxModel doFillMsegItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "N", "N" );	
		    items.add( "Y", "Y" );
		    	  		   
		    return items;
		}
		public ListBoxModel doFillRespItems() {
		    ListBoxModel items = new ListBoxModel();		    
		    items.add( "N", "N" );		  		   
		    items.add( "Y", "Y" );
		    return items;
		}
		
		public ListBoxModel doFillSignItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "N", "N" );	
		    items.add( "Y", "Y" );		    	  		   
		    return items;
		}
		public ListBoxModel doFillL61Items() {
		    ListBoxModel items = new ListBoxModel();		    
		    items.add( "N", "N" );	
		    items.add( "Y", "Y" );
		    return items;
		}
		
		public ListBoxModel doFillMsgItems() {
		    ListBoxModel items = new ListBoxModel();
		    items.add( "SYSINFO", "SYSINFO" );
		    items.add( "NONIOPCB", "NONIOPCB" );		  		   
		    return items;
		}
		
	
		
		public ListBoxModel doFillRtcinqItems(){
			ListBoxModel items = new ListBoxModel();		    
		    items.add( "N", "N" );
		    items.add( "Y", "Y" );
		    return items;
		}
		
		public ListBoxModel doFillApptlsItems(){
			ListBoxModel items = new ListBoxModel();
		    items.add( "Y", "Y" );
		    items.add( "N", "N" );		  		   
		    return items;
		}
		
		public ListBoxModel doFillTraninqItems(){
			ListBoxModel items = new ListBoxModel();		    
		    items.add( "N", "N" );		  		   
		    items.add( "Y", "Y" );
		    return items;
		}
		
		public ListBoxModel doFillRecvItems(){
			ListBoxModel items = new ListBoxModel();
		    items.add( "Y", "Y" );
		    items.add( "N", "N" );		  		   
		    return items;
		}
		
		public ListBoxModel doFillTranfpItems(){
			ListBoxModel items = new ListBoxModel();		    
		    items.add( "N", "N" );	
		    items.add( "Y", "Y" );
		    return items;
		}
		
		public ListBoxModel doFillMperItems(){
			ListBoxModel items = new ListBoxModel();
			items.add( "MULT", "MULT" );
			items.add( "SNGL", "SNGL" );		    		  		   
		    return items;
		}
		
		public ListBoxModel doFillUcItems(){
			ListBoxModel items = new ListBoxModel();
		    items.add( "Y", "Y" );
		    items.add( "N", "N" );		  		   
		    return items;
		}
		
		public ListBoxModel doFillSpadItems(){
			ListBoxModel items = new ListBoxModel();
		    items.add( "STRUNC", "STRUNC" );
		    items.add( "RTRUNC", "RTRUNC" );		  		   
		    return items;
		}
		
		public ListBoxModel doFillMscItems(){
			ListBoxModel items = new ListBoxModel();		   
		    items.add( "N", "N" );		  	
		    items.add( "Y", "Y" );
		    return items;
		}
	
		public ListBoxModel doFillDcItems(){
			ListBoxModel items = new ListBoxModel();
		    items.add( "Y", "Y" );
		    items.add( "N", "N" );		  		   
		    return items;
		}
		
		public ListBoxModel doFillSerItems(){
			ListBoxModel items = new ListBoxModel();		   
		    items.add( "N", "N" );		
		    items.add( "Y", "Y" );
		    return items;
		}
		
		public ListBoxModel doFillAoiItems(){
			ListBoxModel items = new ListBoxModel();
		    items.add( "N", "N" );
		    items.add( "Y", "Y" );	
		    items.add( "T", "T" );
		    items.add( "C", "C" );	
		    return items;
		}
		
		public ListBoxModel doFillTrantlsItems(){
			ListBoxModel items = new ListBoxModel();		    
		    items.add( "N", "N" );	
		    items.add( "Y", "Y" );
		    return items;
		}
	
		
		
		
	
	}

}
