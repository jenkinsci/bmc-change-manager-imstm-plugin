# DevOps for BMC AMI Change Manager for IMS TM
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/bmc-cfa-plugin.svg)](https://plugins.jenkins.io/bmc-cfa-plugin)
# Table of contents
1. [Requirements](#req)
2. [Overview](#overview)
3. [Authenticating the SSL Certificate](#cert)
4. [Screenshots](#screenshots)
5. [Job Logs](#joblogs)
9. [Required information for diagnosing problems](#diag)

## Requirements <a name="req"></a>
| Required software                 | Version |
|-----------------------------------|---------|
| Jenkins                           | 2.349   |
| BMC AMI Change Manager for IMS TM | 3.0     |
| z/OSMF                            |    |

## Overview <a name="overview"></a>
DevOps for BMC AMI Change Manager for IMS TM plugin can be invoked as a Jenkins job's **build step**. It allows assimilation of BMC AMI Change Manager for IMS TM capabilities in Dev/Ops processes. It takes advantage of BMC AMI Change Manager for IMS TM functionality to dynamically activate changes to IMS objects.
![BMC build step](/src/main/webapp/images/bmc_build_step.jpg)

It provides the following capabilities:
- Creating and Editing DELTA Lists.
- Checking a DELTA List against the IMS control region without implementing the changes. The Check function
verifies the elements you have inserted or changed on the DELTA List.
- Executing a DELTA List implements the DELTA List on the IMS control region and logs all of the changes it makes.

The plugin uses z/OSMF REST API to submit the following jobs to run on z/OS.
- DLPBTSCT - Utility that is used to check or execute DELTA List in batch. BMCXLINK task is not required.
- DLPYLIST - Sample user exit to create DELTA List records in batch using control cards.

It uses z/OSMF REST API to submit jobs to run on z/OS.

**Notes!**

- Notice that the values for the different keywords are not explicitly displayed in the generated JCL.
  Instead placeholders (which are marked with ${}) are used.
  The placeholders are being internally resolved into variables by Jenkins during the build process.
  Therefore, it’s not recommended to modify the JCL, but it’s possible.

- **Script Security Plugin** is used, thus an administrator will have to approve the scripts.
  Otherwise a security exception is thrown:
  ERROR: Failed to evaluate groovy script.
  org.jenkinsci.plugins.scriptsecurity.scripts.UnapprovedUsageException: script not yet approved for use

  An administrator will have to approve via Manage Jenkins -> In-process Script Approval

![ In-process Script Approval](https://github.com/jenkinsci/bmc-cfa-plugin/blob/master/src/main/webapp/images/In_process_script_approv.JPG)
![ Script Approval](https://github.com/jenkinsci/bmc-cfa-plugin/blob/master/src/main/webapp/images/ScriptApproval.JPG)


## Authenticating the SSL Certificate <a name="cert"></a>
1. Distribute the z/OS certificate to the appropriate workstation, and import it into Java KeyStore using keytool.
2. Issue the following command from the command line:
```
keytool -import -alias <alias_name> -keystore <keystore_name > -file <file_name>
```
where:
- **alias_name** - alias name of the entry to process
- **keystore_name** - the location of the cacerts file , By default it is in jre/lib/security/cacerts
- **file_name**- file.cer
  You will be asked for password (which is by default : changeit). Enter the password.
  Restart your Java Virtual Machine or your computer.

## Screenshots: <a name="screenshots"></a>
![BMC DLP Plugin](/src/main/webapp/images/dlp_plugin.jpg)

For details of the dialog box fields, click the question mark icon next to each field.


## Job Logs <a name="joblogs"></a>
The contents of the Change Manager for IMS TM job spool files are available in Jenkins job **workspace** under the respective build number folder.

![DLP job logs](/src/main/webapp/images/workspace.jpg)


## Required information for diagnosing problems <a name="diag"></a>
1.	Identify **plugin version**:
      **Jenkins**->**Manage Jenkins**->**Manage Plugins**->**Installed**
      ![BMC plugin version](https://github.com/jenkinsci/bmc-cfa-plugin/blob/master/src/main/webapp/images/plugin_version.JPG)
2.	**Jenkins Job logs**  
      * Navigate to  **C:\Users\\*user_name*\\AppData\Local\Jenkins\.jenkins**
      * Select **jobs** directory
      * Select the relevant job
      * Select **builds** directory
      * Select the relevant build number
      * log
3. System log: **Jenkins**->**Manage Jenkins**->**System Log**
4. **config.xml** in C:\Users\\*user_name*\\AppData\Local\Jenkins\.jenkins\jobs\\*job_name*
