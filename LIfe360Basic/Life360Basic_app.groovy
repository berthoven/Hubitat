/**

 *  ****************  Life 360 Basic App  ****************
 *
 *  Design Usage:
 *  Life360 Basic
 *
 *  Copyright TBD
 *  
 *  This App is free.  If you like and use this app, please be sure to mention it on the Hubitat forums!  Thanks.
 *
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *  Changes:
 *
 *  V1.0.0 - 01/18/21 - Created Life360 Basic initial version
 */

import java.text.SimpleDateFormat

def setVersion(){
	if(logEnable) log.debug "In setVersion - App Watchdog Parent app code"
    // Must match the exact name used in the json file. ie. AppWatchdogParentVersion, AppWatchdogChildVersion
    state.appName = "Life360BasicParentVersion"
	state.version = "v1.0.0"
    
    try {
        if(sendToAWSwitch && awDevice) {
            awInfo = "${state.appName}:${state.version}"
		    awDevice.sendAWinfoMap(awInfo)
            if(logEnable) log.debug "In setVersion - Info was sent to App Watchdog"
	    }
    } catch (e) { log.error "In setVersion - ${e}" }
}

definition(
    name: "Life360 Basic",
    namespace: "BPTWorld",
    author: "berthoven",
    description: "Life360 Basic",
	category: "",
    iconUrl: "",
    iconX2Url: "",
    oauth: [displayName: "Life360Basic", displayLink: "Life360Basic"],
    singleInstance: true,
    importUrl: "",
    ) {
	appSetting "clientId"
	appSetting "clientSecret"
}

preferences {
    page(name: "Credentials", title: "Enter Life360 Credentials", content: "getCredentialsPage", nextPage: "testLife360Connection", install: false)
    page(name: "listCirclesPage", title: "Select Life360 Circle", content: "listCircles", install: false)
    page(name: "life360PlacesPage", title: "Life 360 Places", content: "getLife360PlacesPage", install: false)
    page(name: "customPlacesPage", title: "Custom Places", content: "getCustomPlacesPage", install: false)
}

mappings {
	path("/placecallback") {
		action: [
              POST: "placeEventHandler",
              GET: "placeEventHandler"
		]
	}
    
    path("/receiveToken") {
		action: [
            POST: "receiveToken",
            GET: "receiveToken"
		]
	}
}

def getCredentialsPage() {
    if(logEnable) log.debug "In getCredentialsPage - (${state.version})"
    if(state.life360AccessToken) {
        listCircles()
    } else {
        dynamicPage(name: "Credentials", title: "Enter Life360 Credentials", nextPage: "listCirclesPage", uninstall: true, install:false){
            section(getFormat("header-green", "${getImage("Blank")}"+" Life360 Credentials")) {
    		    input "username", "text", title: "Life360 Username?", multiple: false, required: true
    		    input "password", "password", title: "Life360 Password?", multiple: false, required: true, autoCorrect: false
    	    }
        }
    }
}

def getCredentialsErrorPage(String message) {
    if(logEnable) log.debug "In getCredentialsErrorPage - (${state.version})"
    dynamicPage(name: "Credentials", title: "Enter Life360 Credentials", nextPage: "listCirclesPage", uninstall: uninstallOption, install:false) {
    	section(getFormat("header-green", "${getImage("Blank")}"+" Life360 Credentials")) {
    		input "username", "text", title: "Life360 Username?", multiple: false, required: true
    		input "password", "password", title: "Life360 Password?", multiple: false, required: true, autoCorrect: false
            paragraph "${message}"
    	}
    }
}


def getLife360PlacesPage() {
    if(logEnable) log.debug "In getLife360PlacesPage - (${state.version})"
    dynamicPage(name: "life360PlacesPage", title: "<h2 style='color:#1A77C9;font-weight: bold'>Life 360 Places</h2>", nextPage: "listCirclesPage") {
        section(getFormat("header-green", "${getImage("Blank")}"+" Setup Life 360 Places")) {
            life360Places = state.places.collectEntries{[it.name, it.name + " (Life360)"]}
            sortedPlaces = life360Places.sort()
            input "life360SelectedPlaces", "enum", title: "Life 360 Places", options: sortedPlaces, multiple: true
            input "life360PlaceDelay", "text", title: "Presence Delay", multiple: false, required: true, defaultValue: 120
            input "life360PlaceNotifyArrival", "enum", title: "Arrival Notification", options: ["Push","Speak"], multiple: true
            input "life360PlaceNotifyDeparture", "enum", title: "Departure Notification", options: ["Push","Speak"], multiple: true

        }
    }
}

def getCustomPlacesPage() {
    if(logEnable) log.debug "In customPlacesPage - (${state.version})"
    dynamicPage(name: "customPlacesPage", title: "<h2 style='color:#1A77C9;font-weight: bold'>Custom Places</h2>", nextPage: "listCirclesPage") {
        section(getFormat("header-green", "${getImage("Blank")}"+" Setup Custom Places")) {
            for ( i in 1..10 ) {
                section(getFormat("header-green", "${getImage("Blank")}"+" Custom Place $i")) {
                    input "customPlace" + i + "Name", "text", title: "Place Name", multiple: false, required: false, submitOnChange: true, width: 3
                    if (settings["customPlace" + i + "Name"]) {
                        input "customPlace" + i + "Latitude", "text", title: "Latitude", multiple: false, required: true, width: 3
                        input "customPlace" + i + "Longitude", "text", title: "Longitude", multiple: false, required: true, width: 3
                        input "customPlace" + i + "Radius", "text", title: "Radius", multiple: false, required: true, defaultValue: 250, width: 3
                        input "customPlace" + i + "Delay", "text", title: "Presence Delay", multiple: false, required: true, defaultValue: 120, width: 3
                        input "customPlace" + i + "NotifyArrival", "enum", title: "Arrival Notification", options: ["Push","Speak","History"], multiple: true, width: 3
                        input "customPlace" + i + "NotifyDeparture", "enum", title: "Departure Notification", options: ["Push","Speak","History"], multiple: true, width: 3
                    }
                }
            }
        }
    }
}

def testLife360Connection() {
    if(logEnable) log.debug "In testLife360Connection - (${state.version})"
    if(state.life360AccessToken) {
        if(logEnable) log.debug "In testLife360Connection - Good!"
   		//listCircles()
        true
    } else {
        if(logEnable) log.debug "In testLife360Connection - Bad!"
    	initializeLife360Connection()
    }
}

def initializeLife360Connection() {
    if(logEnable) log.debug "In initializeLife360Connection - (${state.version})"

    initialize()

    def username = settings.username
    def password = settings.password

    def url = "https://api.life360.com/v3/oauth2/token.json"
        
    def postBody =  "grant_type=password&" +
    				"username=${username}&"+
                    "password=${password}"

    def result = null

    try {
       
     		httpPost(uri: url, body: postBody, headers: ["Authorization": "Basic cFJFcXVnYWJSZXRyZTRFc3RldGhlcnVmcmVQdW1hbUV4dWNyRUh1YzptM2ZydXBSZXRSZXN3ZXJFQ2hBUHJFOTZxYWtFZHI0Vg==" ]) {response -> 
     		    result = response
                //if(logEnable) log.debug result
    		}
        if (result.data.access_token) {
            //if(logEnable) log.debug result
       		state.life360AccessToken = result.data.access_token
            return true;
       	}
    	//if(logEnable) log.debug "Life360 initializeLife360Connection, response=${result.data}"
        return ;   
    }
    catch (e) {
       log.error "Life360 initializeLife360Connection, error: $e"
       return false;
    }
}

def listCircles() {
    if(logEnable) log.debug "In listCircles - (${state.version})"
    def uninstallOption = false
    if (app.installationState == "COMPLETE") uninstallOption = true
    dynamicPage(name: "listCirclesPage", title: "<h2 style='color:#1A77C9;font-weight: bold'>Life360 Basic</h2>", install: true, uninstall: true) {
        display()

        // get connected to life360 api
    	if(testLife360Connection()) {
    	    def urlCircles = "https://api.life360.com/v3/circles.json"
 
    	    def resultCircles = null
            //if(logEnable) log.debug "AccessToken: ${state.life360AccessToken}"
       
		    httpGet(uri: urlCircles, headers: ["Authorization": "Bearer ${state.life360AccessToken}" ]) {response -> 
    	         resultCircles = response
		    }

		    //if(logEnable) log.debug "Circles: ${resultCircles.data}"
    	    def circles = resultCircles.data.circles
            
            section(getFormat("header-green", "${getImage("Blank")}"+" Select Life360 Circle")) {
        	    input "circle", "enum", multiple: false, required:true, title:"Life360 Circle", options: circles.collectEntries{[it.id, it.name]}, submitOnChange: true	
            }
            
            if(circles) {
                  state.circle = settings.circle
            } else {
    	        getCredentialsErrorPage("Invalid Usernaname or password.")
            }
        }

        if(circle) {
            if(logEnable) log.debug "In listPlaces - (${state.version})"
            if (app.installationState == "COMPLETE") uninstallOption = true
       
            if (!state?.circle) state.circle = settings.circle

            // call life360 and get the list of places in the circle
            def url = "https://api.life360.com/v3/circles/${state.circle}/places.json"
            def result = null
            httpGet(uri: url, headers: ["Authorization": "Bearer ${state.life360AccessToken}" ]) {response -> 
     	        result = response
            }

            if(logEnable) log.debug "Places=${result.data}" 
            def places = result.data.places
            state.places = places
            

            if(logEnable) log.debug "In listUsers - (${state.version})"

            if (!state?.circle) state.circle = settings.circle

            // call life360 and get list of users (members)
            url = "https://api.life360.com/v3/circles/${state.circle}/members.json"
            result = null
            httpGet(uri: url, headers: ["Authorization": "Bearer ${state.life360AccessToken}" ]) {response -> 
     	        result = response
            }

            //if(logEnable) log.debug "Members=${result.data}"
            // save members list for later
            def members = result.data.members
            state.members = members

            // build preferences page
            section(getFormat("header-green", "${getImage("Blank")}"+" Select Life360 Members to Import into Hubitat")) {
                theMembers = members.collectEntries{[it.id, it.firstName+" "+it.lastName]}
                sortedMembers = theMembers.sort { a, b -> a.value <=> b.value }
        	    input "users", "enum", multiple: true, required:false, title:"Life360 Members: ", options: sortedMembers, submitOnChange: true
            }

            //Create maps of custom locations
            buildLocations()
            def locationMap  = state.locations.collectEntries{[it.name, it.name + " (" + it.type + ")"]}
            sortedLocations = locationMap.sort()

            section(getFormat("header-green", "${getImage("Blank")}"+" Manage Places")) {
                href "life360PlacesPage", title: "Life 360 Places", description: "Click here to manage Life 360 places"
                href "customPlacesPage", title: "Custom Places", description: "Click here to manage custom places"
                paragraph "Please select the location that matches your Hubitat location: ${location.name}"
                input "homeLocation", "enum", multiple: false, required:false, title:"Home Location: ", options: sortedLocations, submitOnChange: true
            }
            
            section(getFormat("header-green", "${getImage("Blank")}"+" Notification Options")) { 
                input "sendPushMessage", "capability.notification", title: "Push Notification Device", multiple: true, required: false
                input "speakerSS", "capability.speechSynthesis", title: "Choose Speech Synthesis speaker(s)", required:false, multiple:true
            }
            
            // ** App Watchdog Code **
            section("This app supports App Watchdog 2! Click here for more Information", hideable: true, hidden: true) {
				paragraph "<b>Information</b><br>See if any compatible app needs an update, all in one place!"
                paragraph "<b>Requirements</b><br> - Must install the app 'App Watchdog'. Please visit <a href='https://community.hubitat.com/t/release-app-watchdog/9952' target='_blank'>this page</a> for more information.<br> - When you are ready to go, turn on the switch below<br> - Then select 'App Watchdog Data' from the dropdown.<br> - That's it, you will now be notified automaticaly of updates."
                input(name: "sendToAWSwitch", type: "bool", defaultValue: "false", title: "Use App Watchdog to track this apps version info?", description: "Update App Watchdog", submitOnChange: "true")
			}
            
            if(sendToAWSwitch) {
                section(getFormat("header-green", "${getImage("Blank")}"+" App Watchdog 2")) {    
                    if(sendToAWSwitch) input(name: "awDevice", type: "capability.actuator", title: "Please select 'App Watchdog Data' from the dropdown", submitOnChange: true, required: true, multiple: false)
			        if(sendToAWSwitch && awDevice) setVersion()
                }
            }
            // ** End App Watchdog Code **
            section(getFormat("header-green", "${getImage("Blank")}"+" Other Options")) {
			    input(name: "logEnable", type: "bool", defaultValue: "false", submitOnChange: "true", title: "Enable Debug Logging", description: "Enable extra logging for debugging.")
    	    }
            display2()
        }
    }
}

def installed() {
    if(logEnable) log.debug "In installed - (${state.version})"
	if(!state?.circle) state.circle = settings.circle
    
    settings.users.each {memberId->
    	// if(logEnable) log.debug "Find by Member Id = ${memberId}"
    	def member = state.members.find{it.id==memberId}

       	// create the device
        if(member) {

          // Modified from @Stephack
            def container = getChildDevices().find{it.typeName == "Life360 Basic Container"}
            if(!container) createContainer(member)
            
            container = getChildDevices().find{it.typeName == "Life360 Basic Container"}
            def childDevice = container.childList()
            if(childDevice.find{it.data.vcId == "${member}"}){
                if(logEnable) log.debug "${member.firstName} already exists...skipping"
            } else {
                if(logEnable) log.debug "Creating Life360 Basic Device: " + member
                try{
                    container.appCreateDevice("${member.firstName}", "Life360 Basic User", "BPTWorld", "${app.id}.${member.id}")
                }
                catch (e) {
                    log.error "Child device creation failed with error = ${e}"
                }
            }
          // end mod
            
            if (childDevice)
        	{
        		if(logEnable) log.debug "Child Device Successfully Created"
     			generateInitialEvent (member, childDevice)
       		}
    	}
    }
    createCircleSubscription()
}

def createCircleSubscription() {
    if(logEnable) log.debug "In createCircleSubscription - (${state.version})"

    if(logEnable) log.debug "Remove any existing Life360 Webhooks for this Circle."

    def deleteUrl = "https://api.life360.com/v3/circles/${state.circle}/webhook.json"

    try { // ignore any errors - there many not be any existing webhooks

    	httpDelete (uri: deleteUrl, headers: ["Authorization": "Bearer ${state.life360AccessToken}" ]) {response -> 
     		result = response}
    		}

    catch (e) {

    	log.debug (e)
    }

    // subscribe to the life360 webhook to get push notifications on place events within this circle

    if(logEnable) log.debug "Create a new Life360 Webhooks for this Circle."

    createAccessToken() // create our own OAUTH access token to use in webhook url
   
    def hookUrl = "${getApiServerUrl()}/${hubUID}/apps/${app.id}/placecallback?access_token=${state.accessToken}"

    def url = "https://api.life360.com/v3/circles/${state.circle}/webhook.json"
        
    def postBody =  "url=${hookUrl}"

    def result = null

    try {
     	httpPost(uri: url, body: postBody, headers: ["Authorization": "Bearer ${state.life360AccessToken}" ]) {response -> 
     	result = response}
    } catch (e) {
        log.debug (e)
    }

    // response from this call looks like this:
    // {"circleId":"41094b6a-32fc-4ef5-a9cd-913f82268836","userId":"0d1db550-9163-471b-8829-80b375e0fa51","clientId":"11",
    //    "hookUrl":"https://testurl.com"}

    //if(logEnable) log.debug "Response = ${result}"

    if (result.data?.hookUrl) {
    	    if(logEnable) log.debug "Webhook creation successful."

    	}
    }

def updated() {
    if(logEnable) log.debug "In updated - (${state.version})"
	if (!state?.circle)
        state.circle = settings.circle

    
    
	if(logEnable) log.debug "In updated() method."
 
    // loop through selected users and try to find child device for each
    settings.users.each {memberId->
    	def externalId = "${app.id}.${memberId}"
        
      // Modified from @Stephack  
        def container = getChildDevices().find{it.typeName == "Life360 Basic Container"}
        if(!container) createContainer(member)
      // end mod
        
		// find the appropriate child device based on my app id and the device network id
        container = getChildDevices().find{it.typeName == "Life360 Basic Container"}
		def deviceWrapper = container.getChildDevice("${externalId}")
        
        if (!deviceWrapper) { // device isn't there - so we need to create
    
    		member = state.members.find{it.id==memberId}
            
          // Modified from @Stephack  
            def childDevice = container.childList()
            if(childDevice.find{it.data.vcId == "${member}"}){
                if(logEnable) log.debug "${member.firstName} already exists...skipping"
            } else {
                if(logEnable) log.debug "Creating Life360 Device: " + member
                try{
                    container.appCreateDevice("${member.firstName}", "Life360 Basic User", "BPTWorld", "${app.id}.${member.id}")
                }
                catch (e) {
                    log.error "Child device creation failed with error = ${e}"
                }
            }
          // end mod
            
        	if (childDevice)
        	{
        		// if(logEnable) log.debug "Child Device Successfully Created"
 				generateInitialEvent (member, childDevice)
       		}
    	}
        else {
          	// if(logEnable) log.debug "Find by Member Id = ${memberId}"
    		def member = state.members.find{it.id==memberId}
        	generateInitialEvent (member, deviceWrapper)
        }
    }

	// Now remove any existing devices that represent users that are no longer selected
    def container = getChildDevices().find{it.typeName == "Life360 Basic Container"}
    def childDevices = container.childList()
    
    if(logEnable) log.debug "Child Devices: ${childDevices}"
    
    childDevices.each {childDevice->
        // log.debug "(l-439) Child = ${childDevice}, DNI=${childDevice.deviceNetworkId}"
        
        def (childAppName, childMemberId) = childDevice.deviceNetworkId.split("\\.")
        //if(logEnable) log.debug "Child Member Id = ${childMemberId}"
        //if(logEnable) log.debug "Settings.users = ${settings.users}"
        if (!settings.users.find{it==childMemberId}) {
            container.deleteChildDevice(childDevice.deviceNetworkId)
            def member = state.members.find {it.id==memberId}
            if (member) state.members.remove(member)
        }
    }
    childDevices = container.childList()
    memberSize = childDevices.size()
    if(logEnable) log.debug "MemberSize: ${memberSize}"
    if(memberSize == 0) {
        if(logEnable) log.debug "Life360 Basic Container has 0 devices - Removing Container"
        deleteChildDevice(container.deviceNetworkId)
    }
}

def generateInitialEvent (member, childDevice) {
    
    if(logEnable) log.debug "In generateInitialEvent - (${state.version})"
    
    subscribe(childDevice, "presentLocation", arrivalHandler)
    subscribe(childDevice, "previousLocation", departureHandler)

    log.debug "Initialising Member=${member} Locations=${state.locations} Home=${settings.homeLocation}"
    childDevice.initialise(state.locations, settings.homeLocation)

    runEvery1Minute(updateMembers)
    //schedule("30 * * * * ?", updateMembers)

}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    if(awDevice) schedule("0 0 3 ? * * *", setVersion)
}

def placeEventHandler() {
	if(logEnable) log.debug "Life360 placeEventHandler: params=$params"
    //if(logEnable) log.debug "Life360 placeEventHandler: settings.place=$settings.place"
    
    def circleId = params?.circleId
    def placeId = params?.placeId
    def userId = params?.userId
    def direction = params?.direction
    def timestamp = params?.timestamp
    
    if (placeId == settings.place) {
		def presenceState = (direction=="in")
		def externalId = "${app.id}.${userId}"
        
        def container = getChildDevices().find{it.typeName == "Life360 Basic Container"}
        
		// find the appropriate child device based on my app id and the device network id
		def deviceWrapper = container.getChildDevice("${externalId}")

		// invoke the generatePresenceEvent method on the child device
		if (deviceWrapper) {
			deviceWrapper.generatePresenceEvent(presenceState, 0)
    		if(logEnable) log.debug "Life360 event raised on child device: ${externalId}"
		}
   		else {
    		log.warn "Life360 couldn't find child device associated with inbound Life360 event."
    	}
    }
}


def arrivalHandler(evt) {

    def presenceDevice = evt.getDevice()
    def user = presenceDevice.getLabel()
    def presentLocation=presenceDevice.currentValue("presentLocation")
    if(logEnable) log.debug "In arrivalHandler: $user / $presentLocation"

    def msg="$user has arrived at $presentLocation"

    def location=getLocation(presentLocation)
    if (location != null) {
        def notify = location.get("notifyArrival")
        if(logEnable) log.debug "In arrivalHandler: notify=$notify"
        if (notify!=null && notify.contains("Push"))    pushHandler(msg)
        if (notify!=null && notify.contains("Speak"))   speechHandler(msg)
        if (notify!=null && notify.contains("History")) presenceDevice.sendHistory(msg)
    }
}

def departureHandler(evt) {
    def presenceDevice = evt.getDevice()
    def user = presenceDevice.getLabel()
    def previousLocation=presenceDevice.currentValue("previousLocation")
    if(logEnable) log.debug "In departureHandler: $user / $previousLocation"

    def msg="$user has departed from $previousLocation"

    def location=getLocation(previousLocation)
    if (location != null) {
        def notify = location.get("notifyDeparture")
        if(logEnable) log.debug "In departureHandler: notify=$notify"
        if (notify!=null && notify.contains("Push"))    pushHandler(msg)
        if (notify!=null && notify.contains("Speak"))   speechHandler(msg)
        if (notify!=null && notify.contains("History")) presenceDevice.sendHistory(msg)
    }
}

def pushHandler(theMessage) {
   	sendPushMessage.deviceNotification(theMessage)
}

def speechHandler(theMessage) {

    def speechDuration = Math.max(Math.round(theMessage.length()/12),2)+3		// Code from @djgutheinz
    def speechDuration2 = speechDuration * 1000

    state.speakers = [speakerSS].flatten().findAll{it}
    state.speakers.each {
        if(logEnable) log.debug "Speaker in use: ${it}"
        it.speak(theMessage)
        pauseExecution(speechDuration2)
    }

    /*
    def speechDuration = Math.max(Math.round(state.theMsg.length()/12),2)+3		// Code from @djgutheinz
    state.speechDuration2 = speechDuration * 1000
    state.speakers = [speakerSS].flatten().findAll{it}
    state.speakers.each {
        if(logEnable) log.debug "Speaker in use: ${it}"
        if(speakerProxy) {
            if(logEnable) log.debug "In letsTalk - speakerProxy - ${it}"
            it.speak(state.theMsg)
        } else if(it.hasCommand('setVolumeSpeakAndRestore')) {
            if(logEnable) log.debug "In letsTalk - setVolumeSpeakAndRestore - ${it}"
            def prevVolume = it.currentValue("volume")
            it.setVolumeSpeakAndRestore(state.volume, state.theMsg, prevVolume)
        } else if(it.hasCommand('playTextAndRestore')) {   
            if(logEnable) log.debug "In letsTalk - playTextAndRestore - ${it}"
            if(volSpeech && (it.hasCommand('setLevel'))) it.setLevel(state.volume)
            if(volSpeech && (it.hasCommand('setVolume'))) it.setVolume(state.volume)
            def prevVolume = it.currentValue("volume")
            it.playTextAndRestore(state.theMsg, prevVolume)
        } else {		        
            if(logEnable) log.debug "In letsTalk - ${it}"
            if(volSpeech && (it.hasCommand('setLevel'))) it.setLevel(state.volume)
            if(volSpeech && (it.hasCommand('setVolume'))) it.setVolume(state.volume)
            it.speak(state.theMsg)
            pauseExecution(state.speechDuration2)
            if(volSpeech && (it.hasCommand('setLevel'))) it.setLevel(volRestore)
            if(volRestore && (it.hasCommand('setVolume'))) it.setVolume(volRestore)
        }
    }
    pauseExecution(state.speechDuration2)
    */
}

def refresh() {
    listCircles()
    updated()
}

def updateMembers(){
    if(logEnable) log.debug "In updateMembers - (${state.version})"
	if (!state?.circle) state.circle = settings.circle
    
    def url = "https://api.life360.com/v3/circles/${state.circle}/members.json"
    def result = null
    sendCmd(url, result)
}

def sendCmd(url, result){ 
    def requestParams = [ uri: url, headers: ["Authorization": "Bearer ${state.life360AccessToken}"]  ]
    asynchttpGet("cmdHandler", requestParams)
}

def cmdHandler(resp, data) {
    
   	def respstatus = resp.getStatus()
    
    if(resp.getStatus() == 200 || resp.getStatus() == 207) {
       
        result = resp.getJson()
	    if(logEnable) log.debug "JSON Results=${result}"

        def members = result.members
    	state.members = members
    
        //Iterate through each user/member
        settings.users.each {memberId->
    
    	//if(logEnable) log.debug "appid $app.id memberid $memberId"	
    
    	def externalId = "${app.id}.${memberId}"
        if(logEnable) log.debug "ExternalId = $externalId"
        
   	    def member = state.members.find{it.id==memberId}
        if(logEnable) log.debug "Processing Member : $member.firstName ($memberId)"

    //if(logEnable) log.debug "member = $member"

	// find the appropriate child device based on my app id and the device network id

    def container = getChildDevices().find{it.typeName == "Life360 Basic Container"}

    def deviceWrapper = container.getChildDevice("${externalId}") 
        if(logEnable) log.debug "Got Child Device Wrapper : ${externalId}"
        
    def location
    def address1
    def address2
    def speed
    def speedMetric
    def speedMiles
    def speedKm
    def xplaces
    def lastUpdated
       
    thePlaces = state.places.sort { a, b -> a.name <=> b.name }
        
    xplaces = "${thePlaces.name}".replaceAll(", ",",")
    
    lastUpdated = new Date()
        
    //log.warn "xplaces: ${xplaces}"
        
    if (member.avatar != null){
        avatar = member.avatar
        avatarHtml =  "<img src= \"${avatar}\">"
        
    } else {
           
        avatar = "not set"
        avatarHtml = "not set"
        }

    if(member.location.name == null || member.location.name == "")
        location = "Unknown"
    else
        location = member.location.name
            
    if(member.location.address1 == null || member.location.address1 == "")
        address1 = "Unknown"
    else
        address1 = member.location.address1
        
    if(member.location.address2 == null || member.location.address2 == "")
        address2 = "Unknown"
    else
        address2 = member.location.address2

        
    //Covert 0 1 to False True	
	def charging = member.location.charge == "0" ? "false" : "true"
    def moving = member.location.inTransit == "0" ? "false" : "true"
	def driving = member.location.isDriving == "0" ? "false" : "true"
	def wifi = member.location.wifiState == "0" ? "false" : "true"
        
    //Fix Iphone -1 speed 
    if(member.location.speed.toFloat() == -1){
        speed = 0
        speed = speed.toFloat()}
    else
        speed = member.location.speed.toFloat()
        
	if(speed > 0 ){
        speedMetric = speed.toDouble().round(2)
        speedMiles = speedMetric.toFloat() * 2.23694
        speedMiles = speedMiles.toDouble().round(2)
        speedKm = speedMetric.toFloat() * 3.6
        speedKm = speedKm.toDouble().round(2)
    }else{
        speedMetric = 0
        speedMiles = 0
        speedKm = 0
    }
                
    def battery = Math.round(member.location.battery.toDouble())
    def latitude = member.location.latitude.toFloat()
    def longitude = member.location.longitude.toFloat()
        
    if(logEnable) log.debug "L360Basic: Updating device for member=$member.firstName"

    //Update the user/device : Also Re-evaluates arrival/departure
    deviceWrapper.setDeviceAttributes(location,address1,address2,battery,charging,member.location.endTimestamp,moving,driving,latitude,longitude,member.location.since,speedMetric,speedMiles,speedKm,wifi,xplaces,avatar,avatarHtml,lastUpdated)

        }  //Each user   
    } // valid JSON response
    else
    {
        log.error "L360Basic: cmdHandler status=${respstatus}"
    }
}

def getLocation(name) {
    return state.locations.find {it.get("name")==name}
}

def buildLocations() {
  state.locations = new ArrayList()
  if(logEnable) log.debug "L360Basic: buildLocations : Building custom locations"
  for ( i in 1..10 ) {
      def placeName = settings["customPlace" + i + "Name"]
      if (placeName) {
          def placeLat = settings["customPlace" + i + "Latitude"].toFloat()
          def placeLong = settings["customPlace" + i + "Longitude"].toFloat()
          def placeRadius = settings["customPlace" + i + "Radius"].toFloat()
          def placeDelay = settings["customPlace" + i + "Delay"].toInteger()
          def notifyArrival = settings["customPlace" + i + "NotifyArrival"]
          def notifyDeparture = settings["customPlace" + i + "NotifyDeparture"]
          def location = buildLocation("Custom",i,placeName,placeLat,placeLong,placeRadius,placeDelay,notifyArrival,notifyDeparture)
          state.locations.add(location)
      }
  }
  if(logEnable) log.debug "L360Basic: buildLocations : Building Life360 locations"
  for (place in state.places) {
      def placeName = place.get("name")
      if (placeName && settings.life360SelectedPlaces?.contains(placeName)) {
          def placeLat = place.get("latitude").toFloat()
          def placeLong = place.get("longitude").toFloat()
          def placeRadius = place.get("radius").toFloat()
          def placeDelay = settings.life360PlaceDelay?:0
          def notifyArrival = settings.life360PlaceNotifyArrival?:[]
          def notifyDeparture = settings.life360PlaceNotifyDeparture?:[]
          def location = buildLocation("Life360",0,placeName,placeLat,placeLong,placeRadius,placeDelay,notifyArrival,notifyDeparture)
          state.locations.add(location)
      }
  }
  state.locations.add(buildLocation("Unknown",0,"Unknown",0,0,0,0,[],[]))
}

def buildLocation(type, index, name, lat, lon, radius, delay, notifyArrival, notifyDeparture) {
    //if(logEnable) log.debug "L360Basic: buildLocation : $type,$index,$name,$lat,$lon,$radius,$delay,$notifyArrival,$notifyDeparture"
    def locationMap = ["type":type,"index":index,"name":name,"latitude":lat,"longitude":lon,"radius":radius,"delay":delay,"notifyArrival":notifyArrival,"notifyDeparture":notifyDeparture]
    return locationMap
}

def createContainer(member){                // Modified from @Stephack
    def container = getChildDevices().find{it.typeName == "Life360 Basic Container"}
    if(!container){
        if(logEnable) log.debug "Creating Life360 Basic Container - (${state.version})"
        try {
            container = addChildDevice("BPTWorld", "Life360 Basic Container", "Life360-${app.id}", null, [name: "Life360-Basic-Members", label: "Life360 Basic Members", completedSetup: true]) 
        } catch (e) {
            log.error "Container device creation failed with error = ${e}"
        }
        //createVchild(container, member)
    }
    //else {createVchild(container, member)}
}

// ********** Normal Stuff **********

def getImage(type) {					// Modified from @Stephack
    def loc = "<img src=https://raw.githubusercontent.com/bptworld/Hubitat/master/resources/images/"
    if(type == "Blank") return "${loc}blank.png height=40 width=5}>"
}

def getFormat(type, myText=""){			// Modified from @Stephack
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
	if(type == "title") return "<div style='color:blue;font-weight: bold'>${myText}</div>"
}

def display() {
	section() {
		paragraph getFormat("line")
	}
}

def display2(){
	setVersion()
	section() {
		paragraph getFormat("line")
		paragraph "<div style='color:#1A77C9;text-align:center'>Life360 Basic - @cwwilson08 & @BPTWorld<br><a href='https://github.com/bptworld/Hubitat' target='_blank'>Find more apps on my Github, just click here!</a><br>Get app update notifications and more with <a href='https://github.com/bptworld/Hubitat/tree/master/Apps/App%20Watchdog' target='_blank'>App Watchdog</a><br>${state.version}</div>"
		paragraph getFormat("line")
	}       
}