/**

 *  ****************  Life 360 Basic Container Driver  ****************
 *
 *  Design Usage:
 *  Life360 Basic
 *  Modified from Virtual Container - Copyright 2018 Stephan Hackett
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


def setVersion(){
    appName = "Life360BasicContainer"
	version = "v1.0.0" 
    dwInfo = "${appName}:${version}"
    sendEvent(name: "dwDriverInfo", value: dwInfo, displayed: true)
}

def updateVersion() {
    log.info "In updateVersion"
    setVersion()
}

metadata {
	definition (
	name: "Life360 Basic Container",
	namespace: "BPTWorld",
	author: "berthoven",
	importUrl: ""
) {
    capability "Actuator"
    capability "Refresh"
    attribute "containerSize", "number"	//stores the total number of child switches created by the container
    command "createDevice", ["DEVICE LABEL", "DRIVER TYPE ", "NAMESPACE ."] //create any new Virtual Device
    attribute "dwDriverInfo", "string"
    command "updateVersion"
    }
}

preferences {
	input name: "about", type: "paragraph", element: "paragraph", title: "Life360 Basic Container", description: "This driver is for use with the Life360 Basic App."
    input(name: "logEnable", type: "bool", defaultValue: "false", submitOnChange: "true", title: "Enable Debug Logging", description: "Enable extra logging for debugging.")
}

def childList(){
	def children = getChildDevices()
    updateSize()
	return children
}

def appCreateDevice(vName, vType, vSpace, vId){
    try{
    	if(logEnable) log.debug "Attempting to create Virtual Device: Namespace: ${vSpace}, Type: ${vType}, Label: ${vName}"
		childDevice = addChildDevice(vSpace, vType, "${vId}", [label: "${vName}", isComponent: false, "vcId": "${vId}"])
    	if(logEnable) log.debug "Success from Life360 Basic Container"
    	updateSize()
    }
    catch (Exception e){
         log.warn "Unable to create device. Please enter a valid driver type!"
    }
}

def refresh() {
	if(logEnable) log.debug "Refreshing Container values"
    updateLabels()
    updateSize()
}

def installed() {
	if(logEnable) log.debug "Installing Life360 Basic Container"
	refresh()
}

def updated() {
    if(logEnable) log.debug "Updating Life360 Basic Container"
	initialize()
}

def initialize() {
	if(logEnable) log.debug "Initializing Life360 Basic Container"
	updateSize()
}

def updateSize() {
	int mySize = getChildDevices().size()
    sendEvent(name:"containerSize", value: mySize)
}

def updateLabels() { // syncs device label with componentLabel data value
    if(logEnable) log.debug "Updating Life360 Basic Container device labels"
    def myChildren = getChildDevices()
    myChildren.each{
        if(it.label != it.data.label) {
            it.updateDataValue("label", it.label)
        }
    }
    updateSize()
}