import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
node {
	stage ("Set configuration variables") {

			script {
				try {
                    def MULE_ENV = ''
                    def KEY = ''
                    def API_ID = ''
                    def ACCESS_TOKEN = ''
                    def BUSINESS_GROUP_NAME = ''
                    def BUSINESS_GROUP_ID = ''
                    def ENVIRONMENT = ''
                    def WORKERS = ''
                    def WORKER_TYPE = ''
                    def REGION = ''
                    def APPLICATION_SUFFIX = ''
                    def ANYPOINT_PLATFORM_URL=''
                    def MULESOFT_USER = ''
                    def MULESOFT_PASSWORD = ''
                   

                    env.BRANCH_NAME='develop';

                    setWorkspaceVariables(env.BRANCH_NAME)
				} catch(Exception e) {
					println "There has been an error setting workspace variables"
					throw e
				}
			}
	}


	stage ("Build & test project") {
			script {
				try {
                    echo "build"
                    build()
				} catch(Exception e) {
					println "There has been an error during testing stage"
					throw e
				}
			}
    }


    stage ("Deploy to Anypoint Platform") {

			script {
				try {
			
                    deploy("")
				} catch(Exception e) {
					println "There has been an error deploying mulesoft API"
					throw e
				}
			}
	}
}


/***************FUNCTIONS**************/

def setWorkspaceVariables(branch) {

    ANYPOINT_PLATFORM_URL = 'eu1.anypoint.mulesoft.com'
    WORKERS = '1'
    WORKER_TYPE = 'Micro'
    REGION = 'eu-central-1'
    MULESOFT_USER = 'josegardu_damm'
    MULESOFT_PASSWORD = 'Cervantes12everis'


    if (branch.equals("master")) {
        MULE_ENV = "PRO"
        ENVIRONMENT = "PRO"
        println "master branch"
    

    } else if (branch.equals("develop")) {
        MULE_ENV= "DEV"
        ENVIRONMENT = "DEV"
        println "develop branch"

    } else {
        println "There is not a workflow specified for this branch"
    }

    APPLICATION_SUFFIX = "-" + MULE_ENV
    
    retrieveMulesoftVariables()
}
@NonCPS
def retrieveMulesoftVariables() {

   
    response = "curl -H 'Content-Type: application/x-www-form-urlencoded' -X POST -d username=${MULESOFT_USER} -d password=${MULESOFT_PASSWORD} https://${ANYPOINT_PLATFORM_URL}/accounts/login".execute().text

    def slurper = new JsonSlurper()

    ACCESS_TOKEN = slurper.parseText(response).access_token
    url = "curl -s -X GET https://${ANYPOINT_PLATFORM_URL}/accounts/api/me -H \"Authorization:Bearer ${ACCESS_TOKEN}\""
    response = slurper.parseText(url.execute().text)

    BUSINESS_GROUP_NAME = response.user.contributorOfOrganizations[0].name
    ANYPOINT_PLATFORM_CLIENT_ID = response.user.contributorOfOrganizations[0].clientId
    BUSINESS_GROUP_ID = response.user.contributorOfOrganizations[0].id


    url = "curl -s -X GET https://${ANYPOINT_PLATFORM_URL}/accounts/api/organizations/${BUSINESS_GROUP_ID}/environments -H \"Authorization:Bearer ${ACCESS_TOKEN}\""
    
    response = slurper.parseText(url.execute().text)


    for(i=0;i < response.data.size();i++){
    
    if(response.data[i].name.equals(ENVIRONMENT)){
        
        ENVIRONMENT_ID = response.data[i].id
        response = null;   
        break;
        
    }
    
}


}

def runMulesoftPipeline(apiName) {
	environment {
        BRANCH_NAME = 'develop'
    }


	stage ("Set configuration variables") {
		container('mule-builder') {
			script {
				try {
                    setWorkspaceVariables(env.BRANCH_NAME)
				} catch(Exception e) {
					println "There has been an error setting workspace variables"
					throw e
				}
			}
		}
	}


	stage ("Build & test project") {
		container('mule-builder') {
			script {
				try {
                    build()
				} catch(Exception e) {
					println "There has been an error during testing stage"
					throw e
				}
			}
		}
    }


    stage ("Deploy to Anypoint Platform") {
		container('mule-builder') {
			script {
				try {
		      		   uploadAsset("")
				} catch(Exception e) {
					println "There has been an error deploying mulesoft API"
					throw e
				}
			}
		}
	}

}

def build() {

      //sh "mvn clean test"
      bat "git clone -b develop https://github.com/rpaulnu/api-dummy-damm.git"
      bat "cd api-dummy-damm & C:/opt/apache-maven-3.6.3/bin/mvn clean test"
    
    
}

def uploadAsset(apiName) {
url = new URL("https://${ANYPOINT_PLATFORM_URL}/apimanager/api/v1/organizations/${BUSINESS_GROUP_ID}/environments/${MULE_ENV}/apis") 
// Set the connection verb and headers
def conn = url.openConnection() 
conn.setRequestMethod("POST") 
conn.setRequestProperty("Content-Type", "application/json")
conn.setRequestProperty("Authorization", "Bearer ${ACCESS_TOKEN}")

// Required to send the request body of our POST 
conn.doOutput = true
	def data = [
    spec: [
        groupId: "0994ed66-9d28-4904-8231-74516966ecdd",
	assetId: "api-dummy-damm",
	version: "1.0.0"
    ],
    endpoint: [
    	uri: "https://some.implementation.com",
	proxyUri: "http://0.0.0.0:8081/",
	isCloudHub: true	
    ],
    instanceLabel: "API de prueba"
]
def body = new JsonBuilder(data)
body = body.toString()
/*Build JSON Body*/
/*def body = jsonBuilder {
           spec {
		 groupId "0994ed66-9d28-4904-8231-74516966ecdd"
		 assetId "api-dummy-damm"
		 version "1.0.0"
		}
	endpoint {
		uri "https://some.implementation.com"
		proxyUri "http://0.0.0.0:8081/"
		isCloudHub true
		}
	instanceLabel "API de prueba"
}*/

		
//body="{\"spec\": {\"groupId\": \"0994ed66-9d28-4904-8231-74516966ecdd\",\"assetId\": \"api-dummy-damm\",\"version\": \"1.0.0\" },\"endpoint\": {\"uri\": \"https://some.implementation.com\",\"proxyUri\": \"http://0.0.0.0:8081/\",\"isCloudHub\": true  },\"instanceLabel\": \"API de prueba\"}"
//body='{"spec": {"groupId": "0994ed66-9d28-4904-8231-74516966ecdd","assetId": "api-dummy-damm","version": "1.0.0" },"endpoint": {"uri": "https://some.implementation.com","proxyUri": "http://0.0.0.0:8081/","isCloudHub": true  },"instanceLabel": "API de prueba"}'

// Create our JSON body  
//Send our request 
conn.getOutputStream()
  .write(body.getBytes("UTF-8"));
def postRC = conn.getResponseCode();
println(postRC);
    println(conn.getInputStream().getText());
/*writer.write(body) 
writer.flush() 
writer.close() 
conn.connect()


println conn.content.text*/

/*print url
url.execute().text*/


/*https://eu1.anypoint.mulesoft.com/apimanager/api/v1/organizations/{{organizationId}}/environments/{{environmentId}}/apis
    /*bat """
        cd api-dummy-damm & C:/opt/apache-maven-3.6.3/bin/mvn -B deploy -DskipTests \
                -Denvironment=${ENVIRONMENT} \
                -Dmule.applicationName=app-api-dummy-damm \
                -Danypoint.username=${MULESOFT_USER} \
                -Danypoint.password=${MULESOFT_PASSWORD} \
                -Danypoint.platform.client_id=${ANYPOINT_PLATFORM_CLIENT_ID} \
                -Danypoint.platform.client_secret=47b84B097A94471799266da97209895A \
                -Dmule.env=${MULE_ENV} \
                -Dmule.businessGroup=${BUSINESS_GROUP_NAME} \
                -DapplicationSuffix=${APPLICATION_SUFFIX} \
                -Dmule.businessGroupId=${BUSINESS_GROUP_ID}
    """*/
}

def deploy(apiName) {

    uploadAsset("")

    /*bat """
        cd api-dummy-damm & C:/opt/apache-maven-3.6.3/bin/mvn -B package deploy -DskipTests -DmuleDeploy \
                -Denvironment=${ENVIRONMENT} \
                -Dmule.region=${REGION} \
                -Dmule.applicationName=app-api-dummy-damm \
                -Danypoint.username=${MULESOFT_USER} \
                -Danypoint.password=${MULESOFT_PASSWORD} \
                -Danypoint.platform.client_id=${ANYPOINT_PLATFORM_CLIENT_ID} \
                -Danypoint.platform.client_secret=47b84B097A94471799266da97209895A \
                -Dmule.env=${MULE_ENV} \
                -Dmule.businessGroup=${BUSINESS_GROUP_NAME} \
                -Dmule.workerType=${WORKER_TYPE} \
                -Dmule.workers=${WORKERS} \
                -DapplicationSuffix=${APPLICATION_SUFFIX} \
                -Dmule.businessGroupId=${BUSINESS_GROUP_ID}
    """*/
}


def notifyBuildStatus(result, emailList) {
    notifyBuild(result, false, emailList)
}

def notifyBuild(buildStatus, qualityGate, emailList) {
    // build status of null means successful
    buildStatus =  buildStatus ?: 'SUCCESSFUL'

    // Default values
    def colorName = 'RED'
    def colorCode = '#FF0000'
    def mailAddr = globalVariables.emailListInternal()
    def sendTo = emailList.equals('')?"${mailAddr}":"${mailAddr},${emailList}"

    // Override default values based on build status
    if (buildStatus == 'STARTED') {
	    color = 'YELLOW'
	    colorCode = '#FFFF00'
    } else if (buildStatus == 'SUCCESSFUL') {
	    color = 'GREEN'
	    colorCode = '#00FF00'
    } else {
	    color = 'RED'
	    colorCode = '#FF0000'
    }

    def subject = "${env.JOB_NAME} - Build #${env.BUILD_NUMBER} - ${buildStatus}!"
    def details = """<p>${env.JOB_NAME} - Build #${env.BUILD_NUMBER} - <b>${buildStatus}</b>.</p><p>Check attachment to view the results.</p>"""
    if (qualityGate) {
	    subject = "${env.JOB_NAME} - Quality Gate ${buildStatus} on Build #${env.BUILD_NUMBER} - !"
	    details = """<p>${env.JOB_NAME} - Quality Gate ${buildStatus} on Build #${env.BUILD_NUMBER} - <b>${buildStatus}</b>.</p><p>Check attachment to view the results.</p>"""
    }


    emailext (
	    subject: subject,
	    body: details,
	    to: sendTo,
	    attachLog: true,
	    recipientProviders: [[$class: 'DevelopersRecipientProvider']]
	    )
}
