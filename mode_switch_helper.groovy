/**
 *  mode_switch_helper v1 for mode_switch virtual device
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
 
definition(
    name: "Mode Switch Helper",
    namespace: "gkl-sf",
    author: "gkl_sf",
    description: "Mode switch helper",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section {
		input "modeSwitcher", "capability.actuator"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(modeSwitcher, "modeChange", deviceModeChanged)
    subscribe(location, "mode", locationModeChanged)
}

def deviceModeChanged(evt) { //device sent new mode to helper
    log.debug "current system mode is ${location.mode}, device requests mode change to ${evt.value}"
    if (location.mode != evt.value) {
        setLocationMode(evt.value)   
    }    
}

def locationModeChanged(evt) { //send mode change to device
    def currentDeviceMode = modeSwitcher.currentValue("modeCurrent")
    log.debug "current device mode is ${currentDeviceMode}, system mode changed to ${evt.value}"
    if (currentDeviceMode != evt.value) {
        modeSwitcher.locationModeChanged(evt.value)
    }
}
