/*
 *  mode_switch virtual device v1, requires mode_switch_helper smartapp
 *
 *  Copyright 2016 gkl_sf
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
	definition (name: "Mode Switch", namespace: "gkl-sf", author: "gkl_sf") {
		capability "Actuator" //capability without built-in commands

        //these are needed for tile actions
        command "modeHome"
        command "modeAway"
        command "modeNight"       
        
        //this is for the helper app to inform the device that system mode was changed
        command "locationModeChanged", ["string"]
        
        attribute "modeCurrent", "string"
        attribute "modeChange", "string"
        
		fingerprint inClusters: "0x91"
        
    }
    
    tiles(scale: 2) {

        standardTile("Home", "device.Home", decoration: "flat", width: 2, height: 2) {
			state "inactive", label:'Home', action:"modeHome", icon:"st.Home.home2", backgroundColor:"#ffffff"
			state "active", label:'Home', action:"modeHome", icon:"st.Home.home2", backgroundColor:"#dcdcdc"
        } 
        
        standardTile("Away", "device.Away", decoration: "flat", width: 2, height: 2) {
			state "inactive", label:'Away', action:"modeAway", icon:"st.nest.nest-away", backgroundColor:"#ffffff"
			state "active", label:'Away', action:"modeAway", icon:"st.nest.nest-away", backgroundColor:"#dcdcdc"
        }

        standardTile("Night", "device.Night", decoration: "flat", width: 2, height: 2) {
			state "inactive", label:'Night', action:"modeNight", icon:"st.Weather.weather4", backgroundColor:"#ffffff"
			state "active", label:'Night', action:"modeNight", icon:"st.Weather.weather4", backgroundColor:"#dcdcdc"
        }

        valueTile("modeCurrent", "device.modeCurrent", decoration: "flat", width: 6, height: 1) {
			state "default", label:'${currentValue}', backgroundColor:"#999999"
        }
        
		main(["modeCurrent"]) //modeCurrent is primary so that list of Things in mobile app shows the current mode
		details(["modeCurrent", "Home", "Away", "Night"])
    }     
}

//received new mode from helper 
def locationModeChanged(newMode) {
    log.debug "received system mode change to ${newMode}"
    def events = []
    events << sendEvent(name: "modeCurrent", value: newMode, displayed: false)
    events << sendEvent(name: newMode, value: "active", displayed: false)
    if (newMode != "Home") events << sendEvent(name: "Home", value: "inactive", displayed: false)
    if (newMode != "Away") events << sendEvent(name: "Away", value: "inactive", displayed: false)
    if (newMode != "Night") events << sendEvent(name: "Night", value: "inactive", displayed: false)
    events
}

//send events for new mode to helper

def modeHome() {
    sendChangedMode("Home")
}

def modeAway() {
    sendChangedMode("Away")
}

def modeNight() {
    sendChangedMode("Night")
}

private sendChangedMode(newMode) {
    log.debug "send new mode to helper, current system mode ${location.mode}, new mode ${newMode}" 
    if (newMode != location.mode) {
        sendEvent(name: "modeChange", value: newMode, descriptionText: "Change Mode to ${newMode}", isStateChange: true)
    }
}
