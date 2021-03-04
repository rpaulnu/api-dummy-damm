import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
node {
    /***SETEAMOS LAS VARIABLES DEL ENTORNO MULE ***/
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
		            def API_AUTODISCOVERY = ''
		    

                   

                    setWorkspaceVariables(params.BRANCH)
				} catch(Exception e) {
					println "There has been an error setting workspace variables"
					throw e
				}
			}
	}


	stage ("Create API Instance") {
			script {
				try {
                    	uploadAsset(params.API_NAME)
				} catch(Exception e) {
					println "There has been an error creating the API Instance"
					throw e
				}
			}		
    }

}



/**PASA POR PARAMETRO EN JENKINS el branch del proyecto para setear el entorno***/
def setWorkspaceVariables(branch) {

    ANYPOINT_PLATFORM_URL = 'eu1.anypoint.mulesoft.com'
    /***SE PASA COMO PARÁMETROS EL USUARIO Y CONTRASEÑA DE MULESOFT***/
    MULESOFT_USER = params.MULE_USER
    MULESOFT_PASSWORD = params.MULE_PASSWORD

    //en función del branch pasado como parámetro setea al entorno MULE correspondiente
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
/*****PETICIONES A ANYPOINT PLATFORM PARA OBTENER LOS DATOS DEL TENANT*****/
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


/*** CREA UNA INSTANCIA EN EL API MANAGER******/
@NonCPS
def uploadAsset(apiName) {
url = new URL("https://${ANYPOINT_PLATFORM_URL}/apimanager/api/v1/organizations/${BUSINESS_GROUP_ID}/environments/${ENVIRONMENT_ID}/apis") 
// Set the connection verb and headers
def conn = url.openConnection() 
conn.setRequestMethod("POST") 
conn.setRequestProperty("Content-Type", "application/json")
conn.setRequestProperty("Authorization", "Bearer ${ACCESS_TOKEN}")

// Required to send the request body of our POST 
conn.doOutput = true
	def data = [
    spec: [
        groupId: "${BUSINESS_GROUP_ID}",
	assetId: "${apiName}",
	version: "1.0.0"
    ],
    endpoint: [
    	uri: "https://some.implementation.com",
	proxyUri: "http://0.0.0.0:8081/",
	isCloudHub: true,
	muleVersion4OrAbove: true
    ],
    instanceLabel: "API de prueba"
]

def body = new JsonBuilder(data)
body = body.toPrettyString()
	

conn.getOutputStream()
  .write(body.getBytes("UTF-8"));
def postRC = conn.getResponseCode().toString();

    /**Comprueba que la instancia esta creada correctamente**/
	if(postRC.equals("201")){
		println "Created"
	}else{
		error("Error while creating the instance")
	}
def autoDiscover = new JsonSlurper()
response = autoDiscover.parseText(conn.getInputStream().getText().toString());


/**obtención del AUTODISCOVERY para inyectarlo dentro de los manifiestos **/
API_AUTODISCOVERY = response.id


response = null

}

