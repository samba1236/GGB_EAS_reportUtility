/**
 * Created by sroy1011
 */
//@Grab('org.apache.ivy:ivy:2.4.0')
@Grab('org.apache.commons:commons-csv:1.2')
import org.apache.commons.csv.CSVParser
import static org.apache.commons.csv.CSVFormat.*

import java.nio.file.*
import java.nio.charset.*

def env = System.getenv()

deployEnv			= env['DEPLOY_ENVIRONMENT']

sshUser       = env['SSH_USER']
sshPassword   = env['SSH_PWD']

dir_permission = env['DIR_PERMISSION']
file_permission = env['FILE_PERMISSION']

debug					= "true".equalsIgnoreCase(env['CI_DEBUG'])

buildReportFile    = env['BUILD_REPORT_FILE']
nonTalendDeployListOverride  = env['NON_TALEND_DEPLOY_LIST_OVERRIDE']

deployHost = env['DEPLOY_HOST']

def exec_server_host = [] 


def buildReport = null
if(buildReportFile != null){
	buildReport = new File(buildReportFile)
}
else{
	buildReport = new File("BuildReport.txt")
}
appendBuildReport(buildReport, "----------------------------------------------------")
appendBuildReport(buildReport, "Report for build tag: " + env["BUILD_TAG"])
appendBuildReport(buildReport, "Jenkins URL: " + env["BUILD_URL"])

//def ms_request = new groovy.json.JsonBuilder()
//def ms_response = null

//required environment variable, stop if not defined
if(deployEnv == null){	
		println "DEPLOY_ENVIRONMENT need to be defined"
		System.exit(1)
}

println "DEPLOY_ENVIRONMENT : $deployEnv"

if(deployHost == null){	
		println "DEPLOY_HOST need to be defined"
		System.exit(1)
}
else{
exec_server_host << deployHost.toString()
}

if(exec_server_host != null && exec_server_host.size() > 0){
	println "exec_server_host : $exec_server_host"
	appendBuildReport(buildReport, "Non Talend component will be deployed to host : '$exec_server_host'")
}
else{
	println "talend execution server host cannot be derived from environment property value: 'DEPLOY_HOST'"
	appendBuildReport(buildReport, "talend execution server host cannot be derived from environment property value: 'DEPLOY_HOST'")
	System.exit(1)
}



//----------------------------
//---------- Step 1 ----------
//----------------------------


println "----------------------------------------------------"
println "-- Step 1: Check directories in $exec_server_host"
println "----------------------------------------------------"

class DirObjects {
	String name, target;
	boolean deployFlag;
}

def dir_list = []  // User overwrite of jobs to process
println "    - DIR_LIST_CSV_FILE=${env['DIR_LIST_CSV_FILE']}"
try{
	Paths.get(env["DIR_LIST_CSV_FILE"]).withReader { reader ->
		CSVParser csv = new CSVParser(reader, DEFAULT.withHeader())
		csv.iterator().each { record ->
			dir_list << ([
				 name         : record.DIR_OBJECT.trim(), 
				 deployFlag   : "true".equalsIgnoreCase(record.DEPLOY_FLAG.trim()),
				 target  			: record.TARGET != null && record.TARGET.trim().length() > 0 ? record.TARGET.trim() : ""
				 ] as DeployObjects)
			}
    }

}catch(all){
    println "Error reading CSV Deploy List: $all"
    System.exit(1)
}
appendBuildReport(buildReport, "Checking directories defined in : ${env['DIR_LIST_CSV_FILE']}")

dir_list.each {
	if(it.deployFlag){
		for(String each : exec_server_host){
			checkDirectories(it.name, it.target,each, buildReport)	
			
		}
	}
	else{
		println "    >> DEPLOY_FLAG for : $it.name is set to $it.deployFlag, skipping file deployment"
	}
}

//----------------------------
//---------- Step 2 ----------
//----------------------------
println "----------------------------------------------------"
println "-- Step 2: Deploy files to $exec_server_host"
println "----------------------------------------------------"

class DeployObjects {
	String name, target;
	boolean deployFlag;
}


Path nonTalendDeployList = Paths.get(env["NON_TALEND_DEPLOY_LIST_CSV_FILE"])

def deploy_list = []  //List of files to be deployed
println "    - NON_TALEND_DEPLOY_LIST_CSV_FILE='${env['NON_TALEND_DEPLOY_LIST_CSV_FILE']}"

//check build list override
if(nonTalendDeployListOverride != null && !"".equals(nonTalendDeployListOverride)){
	nonTalendDeployListOverrideFileName = "**/NON_TALEND_DEPLOY_LIST_OVERRIDE"
	nonTalendDeployListOverrideFile = new FileNameFinder().getFileNames(".", nonTalendDeployListOverrideFileName)
	if(nonTalendDeployListOverrideFile != null && nonTalendDeployListOverrideFile.size() > 0){
		println "NON_TALEND_DEPLOY_LIST_OVERRIDE file found"
		nonTalendDeployListOverrideFile.each{
			println "NON_TALEND_DEPLOY_LIST_OVERRIDE file found : ${it.toString()}"
			nonTalendDeployList = Paths.get(it)
		}
	}
}

try{
	DEPLOY_OBJECT = ""
	DEPLOY_FLAG = true
	TARGET = ""
	
	nonTalendDeployList.withReader { reader ->
		CSVParser csv = new CSVParser(reader, DEFAULT.withHeader())
		Map csvHeader = csv.getHeaderMap()
		for(eachRecord in csv.getRecords()){
			
		  for(Map.Entry<String, Integer> eachHeader in csvHeader.entrySet()){
		  	if(eachHeader.getKey().equalsIgnoreCase("DEPLOY_OBJECT")){
		  		DEPLOY_OBJECT = eachRecord."${eachHeader.getKey()}".trim()
		  	}
		  	if(eachHeader.getKey().equalsIgnoreCase("DEPLOY_FLAG")){
		  		DEPLOY_FLAG = "true".equalsIgnoreCase(eachRecord."${eachHeader.getKey()}".trim())
		  	}
		  	if(eachHeader.getKey().equalsIgnoreCase("TARGET")){
		  		TARGET = eachRecord."${eachHeader.getKey()}" != null && eachRecord."${eachHeader.getKey()}".trim().length() > 0 ? eachRecord."${eachHeader.getKey()}".trim() : ""
		  	}
		 
		  }
		  
			deploy_list << ([
				 name         : DEPLOY_OBJECT,
				 deployFlag		: DEPLOY_FLAG,
				 target				: TARGET
				 ] as DeployObjects)
		}
	}

}catch(java.nio.charset.MalformedInputException mafe){
	appendBuildReport(buildReport, "MalformedInputException encountered on build list")
	appendBuildReport(buildReport, "Most likely there's a non-ascii/non UTF-8 character in the build list, please fix the build list")
	println "MalformedInputException encountered on build list"
	println "Most likely there's a non-ascii/non UTF-8 character in the build list, please fix the build list"
	System.exit(1)
}catch(all){
	println "Error reading CSV Job List: $all"
	System.exit(1)
}

deploy_list.each {
	if(it.deployFlag){
		for(String each : exec_server_host){
			deployRuntimeFile(it.name, it.target,each, buildReport)
		}
	}
	else{
		println "    >> DEPLOY_FLAG for file: $it.name is set to $it.deployFlag, skipping file deployment"
	}
}
def now = Calendar.getInstance(TimeZone.getTimeZone("America/Chicago")).format("yyyy.MM.dd 'at' HH:mm:ss z")

appendBuildReport(buildReport, "Deployment of non-talend component finished at $now")
appendBuildReport(buildReport, "----------------------------------------------------")
appendBuildReport(buildReport, "----------------------------------------------------")
appendBuildReport(buildReport, "")


def checkDirectories(name, target, deployHost, reportFile){

	def suppresssystemout = debug ? false : true
	def dirTarget = null
	def dirPermission = "--mode=777"
	dirTarget = target
	
	if(dir_permission != null && dir_permission.toString().trim().length() > 0){
		dirPermission = "--mode=${dir_permission}"
	}
	
	
	if(debug){
		println "    >> dirTarget : $dirTarget" 
		println "    >> dirPermission : $dirPermission"
		println "    >> name : ${name}"
		println "    >> target : ${target}"
	}
	
	antDir = new AntBuilder()
	
	try{
		antDir.sshexec(
			host:"$deployHost",
			username:"$sshUser",
			password:"$sshPassword",
			trust:"true",
			suppresssystemout:"$suppresssystemout",
			output:"report.txt",
			command:"[ -d $dirTarget ] && echo 'Directory @${deployHost}:$dirTarget verified exists' || mkdir -v -p ${dirPermission} $dirTarget",
			verbose:false)
			
			String report = new File("report.txt").text
			if(debug){
				println "Directory Verification Report : ${report.trim()}"
			}
			appendBuildReport(reportFile, report)

	}catch(all){
		println "Error creating directory $dirTarget. $all"
		appendBuildReport(reportFile, "Error creating directory $dirTarget")
		return
	}
	
	/*
	
	try{
		antDir.sshexec(
			host:"$deployHost",
			username:"$sshUser",
			password:"$sshPassword",
			trust:"true",
			suppresssystemout:"$suppresssystemout",
			output:"report.txt",
			command:"[ -d $dirTarget ] && chmod $dirPermission $dirTarget || echo 'Directory $dirTarget not exists'",
			verbose:false)
			
			String report = new File("report.txt").text
			println "Directory Verification Report : ${report.trim()}"
	}catch(all){
		println "Error changing permission on directory $dirTarget to $dirPermission"
		return
	}
	*/
}

def deployRuntimeFile(fileName, target, deployHost, reportFile){
	
	def suppresssystemout = debug ? false : true
	def scpTarget = null
	def dirPermission = "777"
	
	scpTarget = target
	
	if(file_permission != null && file_permission.toString().trim().length() > 0){
		dirPermission = file_permission
	}
	
		
	if(debug){
		println "    >> fileName : ${fileName}"
		println "    >> target : ${target}"
		println "    >> scpTarget : $scpTarget" 
	}
		
	def fileToDeploy = null
	def fileNameToDeploy = "**/$fileName"
	
	fileToDeploy = new FileNameFinder().getFileNames(".", fileNameToDeploy)
		
	if(debug){
		println "    >> fileToDeploy : ${fileToDeploy.toString()}"
	}
	
	if(fileToDeploy != null && fileToDeploy.size() > 0){
		fileToDeploy.each{
			Path fullFilePath = Paths.get(it)
			if(Files.exists(fullFilePath)){
				if(debug){
					println "    >> fileToDeploy found : ${it}"
				}
				
				fileName = fullFilePath.getFileName()
				checkRemoteFile = null
				if(scpTarget.contains(fileName.toString())){
					checkRemoteFile = scpTarget
				}
				else{
					checkRemoteFile = scpTarget.endsWith(FileSystems.getDefault().getSeparator()) ? (scpTarget + fileName.toString()) : (scpTarget + FileSystems.getDefault().getSeparator() + fileName.toString())
				}
				antScp = new AntBuilder()
				def remoteMD5Value
				def localMD5Value
				try{
					if(debug){
						println "checking md5sum of remote file $checkRemoteFile"
					}
					antScp.sshexec(
						host:"$deployHost",
						username:"$sshUser",
						password:"$sshPassword",
						trust:"true",
						suppresssystemout:"$suppresssystemout",
						output:"${it}.remote.md5",
						command:"[ -f $checkRemoteFile ] && md5sum $checkRemoteFile || echo 'file $checkRemoteFile not exist'",
						verbose:false)
						
						String remoteMD5 = new File("${it}.remote.md5").text
						int firstIndex = remoteMD5.indexOf("$checkRemoteFile")
						remoteMD5Value = remoteMD5.substring(0, firstIndex)
						if(debug){
							println "remoteMD5Value : ${remoteMD5Value.trim()}"
						}
				}catch(all){
					println "Error checking md5sum for remote file"
					remoteMD5Value = ""
				}
				
				try{
					if(debug){
						println "checking md5sum of local file ${it}"
					}
					
					antScp.checksum(
						file:"${it}",
						forceoverwrite:"yes")
					String localMD5 = new File("${it}.MD5").text
					localMD5Value = localMD5
					if(debug){
						println "localMD5Value : ${localMD5Value.trim()}"
					}
				}catch(Exception ex){
					println "Error checking md5sum for local file, skipping file ${it}"
					return
				}
				
				if(!remoteMD5Value.trim().equals(localMD5Value.trim())){
					
					report = "MD5 mismatch for remote file and local file, overwriting remote file : @${deployHost}:$checkRemoteFile"
					
					if(debug){
						println "$report"
					}
					appendBuildReport(reportFile, report)
					
					try{
						if(debug){
							println "deploying ${it} to $scpTarget"
						}
						
						antScp.scp(	
						    trust:"true",
						    localFile:"${it}",
						    todir:"$sshUser:$sshPassword@$deployHost:$scpTarget",
						    verbose:false
						)
						report = "deployment of remote file : $checkRemoteFile completed"
						if(debug){
							println "$report"
						}
						
						appendBuildReport(reportFile, report)
					}catch(all){
						report = "Error moving files ${it} to $scpTarget, skipping file. $all"
						println "$report"
						
						appendBuildReport(reportFile, report)
						
						return
					}
					
					try{
						println "updating permission to deployed object"
						antScp.sshexec(
							host:"$deployHost",
							username:"$sshUser",
							password:"$sshPassword",
							trust:"true",
							command:"chmod -R $dirPermission $checkRemoteFile",
							verbose:false)
					}catch(all){
						println "Error updating permission"
						return
					}
					
				}
				else{
					report =  "MD5 match, skipping deployment of file : @${deployHost}:$checkRemoteFile"
					if(debug){
						println "$report"
					}
					
					appendBuildReport(reportFile, report)
				}
			}
		}
	}
	else{
		report = "    >> file $fileNameToDeploy not found. please verify DEPLOY_OBJECT : $fileName is in SVN project ODF_ODF_CONFIG with all the right properties" 
		println "$report"
		
		appendBuildReport(reportFile, report)
		return
	}
}


def appendBuildReport(reportFile, reportString){
	reportFile.append(reportString)
	reportFile.append(System.getProperty("line.separator"))
}
