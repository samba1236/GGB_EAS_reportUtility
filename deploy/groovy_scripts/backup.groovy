def env = System.getenv()

sshUser       = env['SSH_USER']
sshPassword   = env['SSH_PWD']
deployHost = env['DEPLOY_HOST']

dir_permission = env['DIR_PERMISSION']
file_permission = env['FILE_PERMISSION']

def dirTarget = "/mapr/datalake/ODM/mleccm/tst_631/c360/developer/rso_backup/"
def sourceFile = "/mapr/datalake/ODM/mleccm/tst_631/c360/t_scripts/jars/JsonXmlRSO-1.0.0.jar"


antDir = new AntBuilder()
antScp = new AntBuilder()

try{
		antDir.sshexec(
			host:"$deployHost",
			username:"$sshUser",
			password:"$sshPassword",
			trust:"true",
			command:"[ -d $dirTarget ] && echo 'Directory @${deployHost}:$dirTarget verified exists' || mkdir -v -p ${dir_permission} $dirTarget",
			verbose:false)
			
		

	}catch(all){
		println "Error creating directory $dirTarget."
		return
	}
	
	try{
						
			antScp.scp(	
				 trust:"true",
				 file:"$sshUser:$sshPassword@$deployHost:$sourceFile",
				 todir:"/home/jenkins/workspace/C360_CIRRUS_ETL_DEPLOY_MODULE/CIRRUS_JSON_DEPLOY_TST/ingestion-cirrus/deploy/",
				 verbose:false)
						
					}catch(all){
						report = "Error moving files to Jenkins home, skipping file."
						return
					}
					
	try{
						
			antScp.scp(	
				 trust:"true",
				 localFile:"/home/jenkins/workspace/C360_CIRRUS_ETL_DEPLOY_MODULE/CIRRUS_JSON_DEPLOY_TST/ingestion-cirrus/deploy/JsonXmlRSO-1.0.0.jar",
				 todir:"$sshUser:$sshPassword@$deployHost:$dirTarget",
				 verbose:false)
						
					}catch(all){
						report = "Error moving files to $dirTarget, skipping file."
						return
					}
	
