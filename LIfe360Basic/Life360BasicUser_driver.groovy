/**

 *  ****************  Life 360 Basic User Driver  ****************
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
    appName = "Life360BasicUser"
	version = "v1.0.0" 
    dwInfo = "${appName}:${version}"
    sendEvent(name: "dwDriverInfo", value: dwInfo, displayed: true)
}

def updateVersion() {
    log.info "In updateVersion"
    setVersion()
}

preferences {
	input title:"<b>Life360 Basic User</b>", description:"Note: Any changes will take effect only on the NEXT update or forced refresh.", type:"paragraph", element:"paragraph"
	input name: "units", type: "enum", title: "Distance Units", description: "Miles or Kilometers", required: false, options:["Kilometers","Miles"]
    input "avatarFontSize", "text", title: "Avatar Font Size", required: true, defaultValue: "15"
    input "avatarSize", "text", title: "Avatar Size by Percentage", required: true, defaultValue: "75"

    input "numOfLines", "number", title: "How many lines to display on History Tile (from 1 to 10 only)", required:true, defaultValue: 5
    input "historyFontSize", "text", title: "History Font Size", required: true, defaultValue: "15"
    input "historyHourType", "bool", title: "Time Selection for History Tile (Off for 24h, On for 12h)", required: false, defaultValue: false
    input "logEnable", "bool", title: "Enable logging", required: true, defaultValue: false
} 
 
metadata {
	definition (
	name: "Life360 Basic User",
	namespace: "BPTWorld",
	author: "berthoven",
	importURL: ""
    ) {
        capability "Actuator"
	    capability "Presence Sensor"
	    capability "Sensor"
        capability "Refresh"
	    capability "Sleep Sensor"
        capability "Battery"
        capability "Power Source"

        attribute "presentLocation", "String"        //Official presence location (may be subject to delayed arrival)
        attribute "previousLocation", "String"       //Official prior location (may be subject to delayed departure)
        attribute "presentLocationSince", "number"   //How long have I been present at current location

        attribute "distanceMetric", "Number"
   	    attribute "distanceKm", "Number"
	    attribute "distanceMiles", "Number"

        attribute "location", "String"
        attribute "address1", "String"
  	    attribute "address2", "String"
  	    attribute "battery", "number"
   	    attribute "charge", "boolean" //boolean
   	    attribute "lastCheckin", "number"
       	attribute "inTransit", "String" //boolean
   	    attribute "isDriving", "String" //boolean
   	    attribute "latitude", "number"
   	    attribute "longitude", "number"
   	    attribute "since", "number"
   	    attribute "speedMetric", "number"
        attribute "speedMiles", "number"
        attribute "speedKm", "number"
   	    attribute "wifiState", "boolean" //boolean
        attribute "savedPlaces", "map"
        attribute "avatar", "string"
        attribute "avatarHtml", "string"
        attribute "life360Tile1", "string"
        attribute "history", "string"
        attribute "status", "string"
        attribute "lastMap", "string"
        attribute "lastUpdated", "string"
        attribute "numOfCharacters", "number"
        attribute "lastLogMessage", "string"
        
	    command "refresh"
	    command "asleep"
        command "awake"
        command "toggleSleeping"
        command "setBattery",["number","boolean"]
        command "sendHistory", ["string"]
        command "sendTheMap", ["string"]
        command "historyClearData"
        command "refreshTile"
        
        attribute "dwDriverInfo", "string"
        command "updateVersion"
	}
}

def installed(){
    log.info "Life360 Basic User Installed"
    historyClearData()
}

def updated() {
    log.info "Life360 Basic User has been Updated"
}

def initialise(List locations, String homeLocationName) {
    log.info "L360Basic: initialise locations"
    def oldLocations = state.locations 
    state.homeLocationName = homeLocationName
    
    //Clone the list of locations and attach to this device
    state.locations = locations.collect {it.clone()}
    
    //Merge attributes from the old location data
    if (oldLocations!=null) {
        state.locations.each { location ->
            oldLocation = oldLocations.find {it.get("name")==location.get("name")}
            if (oldLocation!=null) {
                mergeMap(location, oldLocation, "present")
                mergeMap(location, oldLocation, "distance")
                mergeMap(location, oldLocation, "lastHere")
                mergeMap(location, oldLocation, "arrived")
                mergeMap(location, oldLocation, "departed")
            }
        }
    }
    state.locations.each { it -> log.info " > " + it.get("name") + ":  " + it }
}

def mergeMap(targetMap, sourceMap, attribute) {
    def value=sourceMap.get(attribute)
    if (value!=null) targetMap.put(attribute, value)
}

def sendTheMap(theMap) {
    lastMap = "${theMap}" 
    sendEvent(name: "lastMap", value: lastMap, displayed: true)
}

def refreshTile() {
    sendLife360Tile1()
}

def sendLife360Tile1() {
    
    if(logEnable) log.debug "in Life360 Basic User - Making the Avatar Tile"

    def avat = device.currentValue('avatar')
    def add1 = device.currentValue('address1')
    def bLevel = device.currentValue('battery')
    def bCharge = device.currentValue('powerSource')
    def bSpeedKm = device.currentValue('speedKm')
    def bSpeedMiles = device.currentValue('speedMiles')
    def presentLocation = device.currentValue('presentLocation')
    def presentLocationSince = device.currentValue('presentLocationSince')
    def distanceKm = device.currentValue('distanceKm')
    def distanceMiles = device.currentValue('distanceMiles')

    if(presentLocation == null || presentLocation == "Unknown" || presentLocation == "No Data") presentLocation = "Between Places"

    
    def binTransit = device.currentValue('inTransit')
    if(binTransit == "true") {
        binTransita = "Moving"
    } else {
        binTransita = "Not Moving"
    }

    def bWifi = device.currentValue('wifiState')
    if(bWifi == "true") {
        bWifiS = "Wifi"
    } else {
        bWifiS = "No Wifi"
    }

    int sEpoch = presentLocationSince
    if(sEpoch == null) {
        theDate = use( groovy.time.TimeCategory ) {
            new Date( 0 )
        }
    } else {
        theDate = use( groovy.time.TimeCategory ) {
            new Date( 0 ) + sEpoch.seconds
        }
    }
    SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("E hh:mm a")
    String dateSince = DATE_FORMAT.format(theDate)

    theMap = "https://www.google.com/maps/search/?api=1&query=${device.currentValue('latitude')},${device.currentValue('longitude')}"
    
	tileMap = "<table width='100%' valign='top'>"
    tileMap += "<tr><td width='25%'><img src='${avat}' height='${avatarSize}%'></td>"
    tileMap += "<td width='75%'><p style='font-size:${avatarFontSize}px'>At: <a href='${theMap}' style='font-weight:bold' target='_blank'>${presentLocation}</a><br>"
    tileMap += "Since: ${dateSince}<br>"
    tileMap += "Address: ${device.currentValue('address1')}<br>"
    if(units == "Kilometers") tileMap += "${binTransita} - ${bSpeedKm} KMH<br>${distanceKM} KM from home<br>"
    if(units == "Miles") tileMap += "${binTransita} - ${bSpeedMiles} MPH<br>${distanceMiles} Miles from home<br>"
    tileMap += "Battery: ${bLevel}% (${bCharge} / ${bWifiS})</p></td>"
    tileMap += "</tr></table>"
    
	tileDevice1Count = tileMap.length()
	if(tileDevice1Count <= 1000) {
		if(logEnable) log.debug "tileMap - has ${tileDevice1Count} Characters<br>${tileMap}"
	} else {
		log.warn "Life360 - Too many characters to display on Dashboard (${tileDevice1Count})"
	}
	sendEvent(name: "life360Tile1", value: tileMap, displayed: true)
}

def sendHistory(msgValue) {
    if(logEnable) log.trace "In sendHistory - nameValue: ${msgValue}"

    if(msgValue!=null) {
        getDateTime()
        nMessage = newDate + " : " + msgValue

        if(state.list == null) state.list = []
        state.list.add(0,nMessage)  

        if(state.list.size() > 10) state.list = state.list.subList(0, 10)

        String formattedHistory
        String allHistoryString = state.list.join(";")
        logCharCount = allHistoryString.length()
        if(logCharCount > 1000) {
            formattedHistory = "Too many characters to display on Dashboard - ${logCharCount}"
        }
        else {
            formattedHistory="<table><tr><td align='Left'><div style='font-size:${historyFontSize}px'>"
            state.list.take((int)numOfLines).each {it -> formattedHistory += it + "<br>"}
            formattedHistory += "</div></td></tr></table>"
        }

        sendEvent(name: "history", value: formattedHistory, displayed: true)
        sendEvent(name: "numOfCharacters", value: logCharCount, displayed: true)
        sendEvent(name: "lastLogMessage", value: msgValue, displayed: true)
    }
}


def getDateTime() {
	def date = new Date()
	if(historyHourType == false) newDate=date.format("E HH:mm")
	if(historyHourType == true) newDate=date.format("E hh:mm a")
    return newDate
}

def historyClearData(){
	if(logEnable) log.debug "Life360 User Driver - Clearing the data"
    msgValue = "-"
    logCharCount = "0"
	historyLog = "Waiting for Data..."
    sendEvent(name: "history", value: historyLog, displayed: true)
    sendEvent(name: "numOfCharacters1", value: logCharCount1, displayed: true)
    sendEvent(name: "lastLogMessage1", value: msgValue, displayed: true)
}	

def generatePresenceEvent(boolean present) {
	if(logEnable) log.debug "Life360 generatePresenceEvent (present = $present)"

    def presence = formatPresenceText(present)
	def linkText = getLinkText(device)
	def descriptionText = formatDescriptionText(linkText, present)
	def handlerName = getState(present)
	def sleeping = (presence == 'not present') ? 'not sleeping' : device.currentValue('sleeping')
   
    
	if (sleeping != device.currentValue('sleeping')) {
	    sendEvent( name: "sleeping", value: sleeping, descriptionText: sleeping == 'sleeping' ? 'Sleeping' : 'Awake' )
    }
	
    def display = presence + (presence == 'present' ? ', ' + sleeping : '')
    if (display != device.currentValue('display')) {
	    sendEvent( name: "display", value: display,  )
    }
	
	def results = [
		name: "presence",
		value: presence,
		unit: null,
		linkText: linkText,
		descriptionText: descriptionText,
		handlerName: handlerName,
	]
	if(logEnable) log.debug "Generating Event: ${results}"
    
    //Sends a presence event every time, whether presence has changed or not. Try to limit this ?
	sendEvent (results)
}

private setDeviceAttributes(location,address1,address2,battery,charge,endTimestamp,inTransit,isDriving,latitude,longitude,since,speedMetric,speedMiles,speedKm,wifiState,xplaces,avatar,avatarHtml,lastUpdated){

    if(logEnable) log.debug "setDeviceAttributes : Location = $location | Address 1 = $address1 | Address 2 = $address2 | Battery = $battery | Charging = $charge | Last Checkin = $endTimestamp | Moving = $inTransit | Driving = $isDriving | Latitude = $latitude | Longitude = $longitude | Since = $since | Speedmeters = $speedMetric | SpeedMPH = $speedMiles | SpeedKPH = $speedKm | Wifi = $wifiState"

    def curcheckin   = device.currentValue('lastCheckin').toString()
    def curDriving   = device.currentValue('isDriving')
    def curlat       = device.currentValue('latitude').toString()
    def curlong      = device.currentValue('longitude').toString()

    //Check custom locations - and override the Life360 location
    def oldLocation = getPresentLocation()
    evaluateLocations(latitude, longitude)
    def newLocation = getPresentLocation()

    location = newLocation?.get("name")
    if(location != device.currentValue('location'))       sendEvent( name: "location", value: location)
    if(address1 != device.currentValue('address1'))       sendEvent( name: "address1", value: address1)
    if(address2 != device.currentValue('address2'))       sendEvent( name: "address2", value: address2 )   
	if(battery != device.currentValue('battery'))         sendEvent( name: "battery", value: battery )
    if(charge != device.currentValue('charge'))           sendEvent( name: "charge", value: charge )
    if(endTimestamp != curcheckin)                        sendEvent( name: "lastCheckin", value: endTimestamp )
    if(inTransit != device.currentValue('inTransit'))     sendEvent( name: "inTransit", value: inTransit )
    if(isDriving != device.currentValue('isDriving'))     sendEvent( name: "isDriving", value: isDriving )
    if(latitude != curlat)                                sendEvent( name: "latitude", value: latitude )
    if(longitude != curlong)                              sendEvent( name: "longitude", value: longitude )
    if(speedMetric != device.currentValue('speedMetric')) sendEvent( name: "speedMetric", value: speedMetric )
    if(speedMiles != device.currentValue('speedMiles'))   sendEvent( name: "speedMiles", value: speedMiles )
    if(speedKm != device.currentValue('speedKm'))         sendEvent( name: "speedKm", value: speedKm )
    if(wifiState != device.currentValue('wifiState'))     sendEvent( name: "wifiState", value: wifiState )

    setBattery(battery.toInteger(), charge.toBoolean(), charge.toString())

    sendEvent( name: "savedPlaces", value: xplaces )
    sendEvent( name: "avatar", value: avatar )
    sendEvent( name: "avatarHtml", value: avatarHtml )
    sendEvent( name: "lastUpdated", value: lastUpdated.format("MM-dd - h:mm:ss a") )    

    //Evaluate arrival & departure
    evaluateArrivalDeparture(newLocation, oldLocation)
    
    //Evaluate distance from home
    evaluateHomeDistance()

    //Refresh the tile map
    runIn(2, sendLife360Tile1)
}

def evaluateArrivalDeparture(newLocation, oldLocation) {

    if(logEnable) log.debug "L360Basic: evaluateArrivalDeparture - newLocation=$newLocation"
    if(logEnable) log.debug "L360Basic: evaluateArrivalDeparture - oldLocation=$oldLocation"

    def newLocationName  = newLocation?.get("name")?:"Unknown"
    def oldLocationName  = oldLocation?.get("name")?:"Unknown"
    def now=getNow()

    //Check LIVE location
    if(newLocationName != oldLocationName) {

        if(logEnable) log.debug "evaluateArrivalDeparture : Live location has changed (from $oldLocationName to $newLocationName)"
        newLocation?.put("arrived",now)
        oldLocation?.put("departed",now)

    }
    
    //Now check the OFFICIAL location    
    if (newLocation!=null) {

      def presentLocationName = device.currentValue('presentLocation')?:"Unknown"
      def presentLocation     = getLocation(presentLocationName)
        
      //Check if I'm still at the official location
      if (newLocationName != presentLocationName)
      { 
        def update=false
        long arrivedAtNewLocation = newLocation.get("arrived")?:now
        long departedOldLocation  = presentLocation.get("departed")?:now
        long timeAtNewLocation    = (now - arrivedAtNewLocation)
        long timeGoneOldLocation  = (now - departedOldLocation)

        if(logEnable) log.debug "L360Basic: evaluateArrivalDeparture - arrivedAtNewLocation=$arrivedAtNewLocation / departedOldLocation=$departedOldLocation"
          
        //Notify DEPARTURE from existing location (if I've been gone long enough)
        if (newLocationName == 'Unknown' && timeGoneOldLocation >= presentLocation.get("delay")?:0) {
           	if(logEnable) log.debug "L360Basic: $username has departed from $presentLocation"
            update=true
        }

        //Notify ARRIVAL at new location (if I've been there long enough)
        if (newLocationName != 'Unknown' && timeAtNewLocation >= newLocation.get("delay")?:0) {
            if(logEnable) log.debug "L360Basic: $username has arrived at $newLocationName $newLocation"
            update=true
        }
        
        if (update) {
            sendEvent( name: "previousLocation", value: presentLocationName, isStateChange:true)
            sendEvent( name: "presentLocation", value: newLocationName, isStateChange:true)
            sendEvent( name: "presentLocationSince", value: arrivedAtNewLocation, isStateChange:true)
	        sendEvent( name: "lastLocationUpdate", value: "Last location update on:\r\n${formatLocalTime("MM/dd/yyyy @ h:mm:ss a")}" )

            //Official location has changed, regenerate presence
            generatePresenceEvent(newLocationName==state.homeLocationName)
        }      
      }
    }
}

def evaluateHomeDistance() {

    def homeLocation = getLocation(state.homeLocationName)
    if (homeLocation!=null) {

      def homeDistance = homeLocation.get("distance")?:0
      if(logEnable) log.debug "evaluateHomeDistance : Distance = $homeDistance"
    
      def km = sprintf("%.2f", homeDistance / 1000)
      if(km.toDouble().round(2) != device.currentValue('distanceKm')){
          sendEvent( name: "distanceKm", value: km.toDouble().round(2) )
      }

      def miles = sprintf("%.2f", (homeDistance / 1000) / 1.609344)
	  if(miles.toDouble().round(2) != device.currentValue('distanceMiles')){    
          sendEvent( name: "distanceMiles", value: miles.toDouble().round(2) )
      }

      if(homeDistance.toDouble().round(2) != device.currentValue('distanceMetric')){
  	    sendEvent( name: "distanceMetric", value: homeDistance.toDouble().round(2) )
      }
    }
}

def setMemberId (String memberId) {
   if(logEnable) log.debug "MemberId = ${memberId}"
   state.life360MemberId = memberId
}

def getMemberId () {
	if(logEnable) log.debug "MemberId = ${state.life360MemberId}"
    return(state.life360MemberId)
}

def getNow() {
   	def now = new Date()
    long unxNow = now.getTime()
    unxNow = unxNow/1000
    return unxNow
}

private String formatPresenceText(boolean present) {
	if (present)
	return "present"
	else
	return "not present"
}

private formatDescriptionText(String linkText, boolean present) {
	if (present)
		return "Life360 User $linkText has arrived"
	else
	return "Life360 User $linkText has left"
}

private getState(boolean present) {
	if (present)
		return "arrived"
	else
	return "left"
}

private toggleSleeping(sleeping = null) {
	sleeping = sleeping ?: (device.currentValue('sleeping') == 'not sleeping' ? 'sleeping' : 'not sleeping')
	def presence = device.currentValue('presence');
	
	if (presence != 'not present') {
		if (sleeping != device.currentValue('sleeping')) {
			sendEvent( name: "sleeping", value: sleeping,  descriptionText: sleeping == 'sleeping' ? 'Sleeping' : 'Awake' )
		}
		
		def display = presence + (presence == 'present' ? ', ' + sleeping : '')
		if (display != device.currentValue('display')) {
			sendEvent( name: "display", value: display )
		}
	}
}

def asleep() {
	toggleSleeping('sleeping')
}

def awake() {
	toggleSleeping('not sleeping')
}

def refresh() {
	parent.refresh()
return null
}

def setBattery(int percent, boolean charging, charge){
	if(percent != device.currentValue("battery"))
		sendEvent(name: "battery", value: percent);
    
def ps = device.currentValue("powerSource") == "BTRY" ? "false" : "true"
if(charge != ps)
		sendEvent(name: "powerSource", value: (charging ? "DC":"BTRY"));
}

private formatLocalTime(format = "EEE, MMM d yyyy @ h:mm:ss a z", time = now()) {
	def formatter = new java.text.SimpleDateFormat(format)
	formatter.setTimeZone(location.timeZone)
	return formatter.format(time)
}


def getLocation(name) {
    return state.locations.find {it.get("name")==name}
}

def getPresentLocation() {
    return state.locations.find { it.get("present") }
}

def evaluateLocations(my_lat, my_lon) {
    state.locations.each { it -> evaluateLocation(it,my_lat, my_lon) }
    def presentLocation = state.locations.find { it.get("present") }
    if (presentLocation==null) presentLocation = getLocation("Unknown")
    if (presentLocation!=null) {
        presentLocation.put("distance",0)
        presentLocation.put("present",true)
        presentLocation.put("lastHere",getNow())
    }
    return presentLocation
}

def evaluateLocation(location, my_lat, my_lon) {
    if (location != null) {
        def placeLat = location.get("latitude")
        def placeLong = location.get("longitude")
        def placeRadius = location.get("radius")
        def distance = distanceFrom(my_lat, my_lon, placeLat, placeLong)
        def present = distance<=placeRadius
        location.put("distance",distance)
        location.put("present",present)
        if (present) location.put("lastHere",getNow())
        if(logEnable) log.debug "L360Basic: evaluateLocation : $location"
    }
}

def distanceFrom(my_lat, my_lon, place_lat, place_lon) {
    def distance = haversine(my_lat, my_lon, place_lat, place_lon)*1000 // in meters
    return distance
}

def withinRange(my_lat, my_lon, place_lat, place_lon, radius) {
    return (distanceFrom(my_lat, my_lon, place_lat, place_lon) < radius)
}

def haversine(lat1, lon1, lat2, lon2) {
    def R = 6372.8
    // In kilometers
    def dLat = Math.toRadians(lat2 - lat1)
    def dLon = Math.toRadians(lon2 - lon1)
    lat1 = Math.toRadians(lat1)
    lat2 = Math.toRadians(lat2)
 
    def a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2)
    def c = 2 * Math.asin(Math.sqrt(a))
    def d = R * c
    return(d)
}

    
