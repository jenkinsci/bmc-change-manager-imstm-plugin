package com.bmc.ims;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hudson.AbortException;
import hudson.model.TaskListener;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.Charset;

import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;

public class JCLService implements Serializable {
	private String encodedCredentials = null;
	private String authHeader = null;
	private String hostUrl = null;
	private final boolean debug;
	//private String cookie = null;

	public JCLService(boolean debug) {
		super();
		this.debug = debug;
	}

	/*
	 * 
	 */
	/**
	 * Call a random z/OSMF REST API that requires credentials (in this case -
	 * z/OSMF information retrieval service) for validating the connection, and
	 * obtaining authentication token on user login. The user can then include a
	 * token in the header on subsequent requests to the z/OSMF REST services API.
	 * 
	 * @param host
	 *	z/OS host name
	 * @param port
	 *	port number
	 * @param user
	 * 	user name
	 * @param pw
	 *	user password
	 * @param listener
	 *	task listener that receives potentialy failure events
	 * @throws AbortException
	 * 	abort exception
	 */
	public void login(String host, String port, String user, String pw, TaskListener listener) throws AbortException {

		String toEncode = user + ":" + pw;
		this.encodedCredentials = Base64.getEncoder().encodeToString(toEncode.getBytes(Charset.forName("UTF-8")));
		boolean displayError = false;
		hostUrl = "https://" + host + ":" + port;
		String validateUrl = hostUrl + "/zosmf/info";

		try {
			HttpsURLConnection conn = (HttpsURLConnection) new URL(validateUrl).openConnection();

			// Set timeouts to ensure that the connection fails when the external resource
			// isn't available in timely manner. The values are in milliseconds e.g. 5 sec
			// timeout for both connect and read would be:
//			int TIMEOUT = 5000;
//			conn.setConnectTimeout(TIMEOUT);
//			conn.setReadTimeout(TIMEOUT);



			// set http method
			conn.setRequestMethod("GET");

			// set HTTP headers
			conn.addRequestProperty("Authorization", "Basic " + this.encodedCredentials);
			// this enables the CORS access to z/OSMF from Java
			conn.addRequestProperty("x-csrf-zosmf-header", "");

			int responseCode = conn.getResponseCode();
			String replyText = conn.getResponseMessage();
			
			// obtain authentication token (LTPA) on user login
			//this.cookie = conn.getHeaderField("Set-Cookie");

			if (debug) {
				listener.getLogger().println(
						"HTTPS connection response code: " + responseCode + " and response message: " + replyText);
			}
			if (responseCode >= 200 && responseCode <= 299) {
				listener.getLogger().println("Successfully Connected to host " + hostUrl);
				// save credentials
				this.authHeader = "Basic " + this.encodedCredentials;
			} else {
				// try and display something anyway...
				displayError = true;
			}

			if (displayError) {
				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
				String inputLine;
				StringBuffer response = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				// print result
				String message = "Server returned " + responseCode + "\n" + response.toString();
				if (debug) {
					listener.getLogger().println(message);
				}
				throw new AbortException();
			}
		} catch (IOException ex) {
			// TODO: Add catch code
			listener.getLogger().println("Connection to " + hostUrl + " failed");
			ex.printStackTrace();
			throw new AbortException(ex.getMessage());
		}
	}

	/**
	 * Create a single method to handle any kind of https request.
	 * 
	 * @param conn
	 *            is the active HttpsURLConnection
	 * @param url
	 *            is the rest api we'll call
	 * @param method
	 *            is one of PUT, POST, GET, etc.
	 * @param requestBody
	 *            is an optional String containing data to pass with the request
	 * @param headers
	 *            is an optional set of properties with header information.
	 * @return
	 */
	private ResponseObject doGenericRequest(HttpURLConnection conn, String url, String method, String requestBody,
			Properties headers, TaskListener listener) {
		ResponseObject rc = new ResponseObject();

		// call zosMF RestAPI
		try {
			// add any headers passed in..
			if (headers != null) {
				Set keys = headers.keySet();
				for (Object key : keys) {
					String tmp = headers.getProperty(key.toString());
					conn.addRequestProperty(key.toString(), tmp);
				}

				// always include the auth and cors headers
				conn.addRequestProperty("Authorization", this.authHeader);
				conn.addRequestProperty("x-csrf-zosmf-header", "");
				
				// If a a single sign-on or stateful technique is used then include the LTPA token  instead of the basic HTTP authorization header in the header on subsequent requests to the z/OSMF
				// REST services API
				//https://www.ibm.com/support/knowledgecenter/en/SSLTBW_2.1.0/com.ibm.zos.v2r1.izua700/IZUHPINFO_RESTServices.htm
				//conn.addRequestProperty("Set-Cookie", this.cookie);

				// for POST where url contains workflows add Connection: close
				// todo, figure out if this works ok in the plugin...
				// we don't want a tool that only works with this proxy :)
				if (method.startsWith("P") && url.contains("workflows")) {
					conn.addRequestProperty("Connection", "close");
				}
			}

			if (method.equals("POST")) {

				if (requestBody != null && requestBody.toString().length() > 0) {
					conn.setDoOutput(true);

					// for now assume JSON
					// conn.addRequestProperty("Content-Type", "applicaton/json");
					// conn.addRequestProperty("Content-Length", new
					// Integer(requestBody.length()).toString());

					// create an outputStream and write to it
					OutputStream os = conn.getOutputStream();
					os.write(requestBody.getBytes(Charset.forName("UTF-8")));
					os.flush();
					os.close();
				}
			}

			// Submit the job and get job id and jobname.
			if (method.equals("PUT") && requestBody != null && url.contains("/zosmf/restjobs/jobs")) {
				OutputStream os = null;
				try {
					// at this moment just assume plain text
					conn.addRequestProperty("Content-Type", "text/plain");
					// create an outputStream and write to it
					conn.setDoOutput(true);
					os = conn.getOutputStream();
					os.write(requestBody.getBytes(Charset.forName("UTF-8")));

					if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 299) {
						if (debug)
							listener.getLogger().println("Job is successfully written to JES internal reader...");
					} else {
						try {
							InputStream istream = null;
							istream = conn.getErrorStream();
							if (istream != null) {
								BufferedReader in = null;
								StringBuffer response = new StringBuffer();
								String inputLine;
								in = new BufferedReader(new InputStreamReader(istream, "utf-8"));
								while ((inputLine = in.readLine()) != null) {
									response.append(inputLine);
									response.append("\r\n");
								}
								in.close();
								if (debug) {
									listener.getLogger()
											.println("Response of Error Stream Data:\n" + response.toString());
								}
								// JSONObject resp = new JSONObject(response.toString());
								// if (resp.getString("message").contains("Job input was not recognized by
								// system as a job")) {
								rc.ret_code = 16;
								// }
							}
						} catch (IOException ioex) {
							listener.getLogger()
									.println("JCLService: IO Error occured while reading error stream data");
							ioex.printStackTrace();
							// if (debug)
							listener.getLogger().println(ioex);
							// if (ioex.getMessage() != null)
							// listener.getLogger().println(ioex.getMessage());
							// rc.resp_details = ACMConst.BMCAMA00050E;
							rc.ret_code = 8;
						}
					}
				} catch (IOException ioex) {
					listener.getLogger().println("JCLService: IO Error occured while writing to JES internal reader");
					ioex.printStackTrace();
					// if (debug)
					listener.getLogger().println(ioex);
					// if (ioex.getMessage() != null)
					// listener.getLogger().println(ioex.getMessage());
					// rc.resp_details = ACMConst.BMCAMA00073E;
					rc.ret_code = 8;
				} finally {
					if (os != null) {
						try {
							os.flush();
							os.close();
						} catch (IOException ioex) {
							listener.getLogger().println("JCLService: IO error occured while closing outputStream");
							ioex.printStackTrace();
							// if (debug)
							listener.getLogger().println(ioex);
							// if (ioex.getMessage() != null)
							// listener.getLogger().println(ioex.getMessage());
							// rc.resp_details = ACMConst.BMCAMA00113E;
							rc.ret_code = 8;
						}
					}
				}

				// Below logic to get the job name and job id from response after PUT
				if (rc.ret_code != 8 && rc.ret_code != 16) {
					try {
						InputStream istream = conn.getInputStream();
						if (istream != null) {
							BufferedReader br = null;
							StringBuffer sb = new StringBuffer();
							String line = null;
							br = new BufferedReader(new InputStreamReader(istream, "utf-8"));
							while ((line = br.readLine()) != null) {
								sb.append(line);
								sb.append("\r\n");
							}
							br.close();
							if (debug) {
								listener.getLogger().println("Response from Job Submission:\n" + sb.toString());
							}
                            /*
							JSONObject resp = new JSONObject(sb.toString());
							rc.jobId = resp.getString("jobid");
							rc.jobName = resp.getString("jobname");
//							rc.jobOwner = resp.getString("owner");
							rc.jobStatus=resp.getString("status");
//							rc.jobType = resp.getString("type");
//							rc.numOfSpoolFiles= String.valueOf(resp.length());

                             */
                            Gson gson = new Gson();

                            JsonObject resp =gson.fromJson(sb.toString(), JsonObject.class);

                            rc.jobId = resp.get("jobid").getAsString();
                            rc.jobName = resp.get("jobname").getAsString();
                            rc.jobStatus=resp.get("status").getAsString();
						}
					} catch (IOException ioex) {
						listener.getLogger().println("JCLServices: IO Error occured while reading response data");
						ioex.printStackTrace();
						// if (debug)
						listener.getLogger().println(ioex);
						// if (ioex.getMessage() != null)
						// listener.getLogger().println(ioex.getMessage());
						// rc.resp_details = ACMConst.BMCAMA00050E;
						rc.ret_code = 8;
					}
				}
			}

			// GET method to obtain the job status of submitted job
			if (method.equals("GET") && requestBody.contains("Obtain Job Status") && !url.contains("/records")
					&& !url.contains("/restfiles")) {
				try {
					// at this moment just assume plain text
					conn.addRequestProperty("Content-Type", "text/plain");

					InputStream istream = null;
					if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 299) {
						if (debug)
							listener.getLogger().println("Read the response data from getInputStream:");
						istream = conn.getInputStream();
						if (istream != null) {
							BufferedReader in = null;
							StringBuffer response = new StringBuffer();
							String inputLine;
							in = new BufferedReader(new InputStreamReader(istream, "utf-8"));
							while ((inputLine = in.readLine()) != null) {
								response.append(inputLine);
								response.append("\n");
							}
							in.close();
							//JSONObject resp = new JSONObject(response.toString());
                            JsonObject resp = JsonParser.parseString(response.toString()).getAsJsonObject();
                            //rc.jobName = resp.getString("jobname");
							//rc.jobId = resp.getString("jobid");
                            rc.jobName = resp.get("jobname").getAsString();
                            rc.jobId = resp.get("jobid").getAsString();

	//						rc.jobOwner = resp.getString("owner");
							//rc.jobStatus = resp.getString("status");
                            rc.jobStatus = resp.get("status").getAsString();
	//						rc.jobType = resp.getString("type");
	//						if (resp.getString("status").equals("OUTPUT")) {
                            if (resp.get("status").getAsString().equals("OUTPUT")) {
	//							rc.jobRetCode = resp.getString("retcode");
								if (debug)
									listener.getLogger().println("Response from Job Status:\n " + response.toString());
							}
						}
					} else {
						if (debug)
							listener.getLogger().println("Read the response data from getErrorStream:");
						istream = conn.getErrorStream();
						if (istream != null) {
							BufferedReader in = null;
							StringBuffer response = new StringBuffer();
							String inputLine;
							in = new BufferedReader(new InputStreamReader(istream, "utf-8"));
							while ((inputLine = in.readLine()) != null) {
								response.append(inputLine);
								response.append("\n");
							}
							in.close();
							if (debug)
								listener.getLogger()
										.println("Response data of error stream data:\n" + response.toString());
						}
					}
				} catch (IOException ioex) {
					listener.getLogger().println("JCLServices: IO Error occured while reading the response data");
					ioex.printStackTrace();
					listener.getLogger().println(ioex);
					// if (ioex.getMessage() != null)
					// rc.resp_details = ACMConst.BMCAMA00050E;
					rc.ret_code = 8;
				}
			}

			// GET method to list spool files
			if (method.equals("GET") && requestBody.contains("List spool files")) {
				try {
					// at this moment just assume plain text
					conn.addRequestProperty("Content-Type", "text/plain");

					InputStream istream = null;
					if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 299) {
						if (debug)
							listener.getLogger().println("Read the response data from getInputStream:");
						istream = conn.getInputStream();
						if (istream != null) {
							BufferedReader in = null;
							StringBuffer response = new StringBuffer();
							String inputLine;
							in = new BufferedReader(new InputStreamReader(istream, "utf-8"));
							while ((inputLine = in.readLine()) != null) {
								response.append(inputLine);
								response.append("\n");
							}
							in.close();
							//JSONArray resp = new JSONArray(response.toString());
							//JSONObject singleSpoolFile;
                            Gson gson = new Gson();

                            // Define the type of the target List
                            Type listType = new TypeToken<List<spoolObj>>() {}.getType();

                            // Convert the JSON array string to a List of MyObject
                            List<spoolObj> myObjectList = gson.fromJson(response.toString(), listType);

                            for (spoolObj obj : myObjectList) {
                                rc.ddnamevalarr.add(obj.ddname);
                                rc.idvalarr.add(obj.id);
                            }
						}
					} else {
						if (debug)
							listener.getLogger().println("Read the response data from getErrorStream:");
						istream = conn.getErrorStream();
						if (istream != null) {
							BufferedReader in = null;
							StringBuffer response = new StringBuffer();
							String inputLine;
							in = new BufferedReader(new InputStreamReader(istream, "utf-8"));
							while ((inputLine = in.readLine()) != null) {
								response.append(inputLine);
								response.append("\n");
							}
							in.close();
							if (debug)
								listener.getLogger()
										.println("Response data of error stream data:\n" + response.toString());
						}
					}
				} catch (IOException ioex) {
					listener.getLogger().println("JCLServices: IO Error occured while reading the response data");
					ioex.printStackTrace();
					listener.getLogger().println(ioex);
					// if (ioex.getMessage() != null)
					// rc.resp_details = ACMConst.BMCAMA00050E;
					rc.ret_code = 8;
				}
			}

			// GET method to retrieve the contents of a job spool file
			if (method.equals("GET") && requestBody.contains("Retrieve spool files content")) {
				try {
					// at this moment just assume plain text
					conn.addRequestProperty("Content-Type", "text/plain");

					InputStream istream = null;
					if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 299)
					{
						if (debug)
							listener.getLogger().println("Read the response data from getInputStream:");
						rc.istream = conn.getInputStream();
						/*
						if (rc.istream != null) {
							BufferedReader in = null;
							StringBuffer response = new StringBuffer();
							String inputLine;
							in = new BufferedReader(new InputStreamReader(istream, "utf-8"));
							while ((inputLine = in.readLine()) != null) {
								response.append(inputLine);
								response.append("\n");
							}
							in.close();
							//rc.append_data = response;

						}*/

					} else {
						if (debug)
							listener.getLogger().println("Read the response data from getErrorStream:");
						istream = conn.getErrorStream();
						if (istream != null) {
							BufferedReader in = null;
							StringBuffer response = new StringBuffer();
							String inputLine;
							in = new BufferedReader(new InputStreamReader(istream, "utf-8"));
							while ((inputLine = in.readLine()) != null) {
								response.append(inputLine);
								response.append("\n");
							}
							in.close();
							if (debug)
								listener.getLogger()
										.println("Response data of error stream data:\n" + response.toString());
						}
					}
				} catch (IOException ioex) {
					listener.getLogger().println("JCLServices: IO Error occured while reading the response data");
					ioex.printStackTrace();
					listener.getLogger().println(ioex);
					// if (ioex.getMessage() != null)
					// rc.resp_details = ACMConst.BMCAMA00050E;
					rc.ret_code = 8;
				}
			}

			int responseCode = conn.getResponseCode();
			String statusLine = "HTTP/1.1 " + responseCode + " " + conn.getResponseMessage() + "\r\n";
			if (debug) {
				listener.getLogger().println("HTTPS response status: " + statusLine);
			}
			StringBuffer response = new StringBuffer();

			response.append(statusLine);
			addHeaders(response, conn.getHeaderFields());
			response.append("\r\n");
			rc.status = responseCode;
			rc.statAndHeaders = response;

		} catch (MalformedURLException murle) {
			// TODO: Add catch code
			murle.printStackTrace();
		} catch (ProtocolException pe) {
			// TODO: Add catch code
			pe.printStackTrace();
		} catch (IOException ioe) {
			// TODO: Add catch code
			ioe.printStackTrace();
		}
		return rc;
	}

	/**
	 * Setup the secure connection and do the request.
	 * 
	 * @param url
	 *	z/OS url
	 * @param method
	 * 	PUT/GET
	 * @param requestBody
	 *	http request's body
	 * @param headers
	 *	http request'd header
	 * @return
	 *	ResponseObject
	 */
	public ResponseObject doRequest(String url, String method, String requestBody, Properties headers,
			TaskListener listener) {
		ResponseObject rc = null;

		// call zosMF RestAPI
		try {
			HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
			//conn.setHostnameVerifier(new TrustMatchHostNameVerifier() {});

			conn.setRequestMethod(method);
			rc = this.doGenericRequest(conn, url, method, requestBody, headers, listener);
			// conn.disconnect();

		} catch (MalformedURLException murle) {
			// TODO: Add catch code
			murle.printStackTrace();
		} catch (ProtocolException pe) {
			// TODO: Add catch code
			pe.printStackTrace();
		} catch (IOException ioe) {
			// TODO: Add catch code
			ioe.printStackTrace();
		}
		return rc;
	}

	/**
	 * Create a method for adding headers easily.
	 * 
	 * @param resp
	 * @param map
	 */
	private void addHeaders(StringBuffer resp, Map<String, List<String>> map) {
		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			if (entry.getKey() != null) {

				// entry.getValue returns an array, splice it into a string.
				List<String> values = entry.getValue();
				String value = "";
				ListIterator li = values.listIterator();
				boolean first = true;
				while (li.hasNext()) {
					if (!first) {
						value = value + "; ";
					} else {
						first = !first;
					}
					String tmp = (String) li.next();
					value = value.concat(tmp);
				}
				resp.append(entry.getKey() + ": " + value + "\n");
			}
		}
	}

}

class spoolObj {
    String id;
    String ddname;

    // Constructors, getters, and setters
    private spoolObj(String id, String ddname) {
        this.id = id;
        this.ddname = ddname;
    }
}

