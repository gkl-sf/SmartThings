/**
 *  Protection Mode helper -- use a virtual switch to control Cooper Aspire RF9540 Protection Mode
 *  Requires RF9540 custom DH with Protection Mode 
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
 *
 *  07 May 2016  v1 initial release
 *
 */
 
definition(
    name: "Protection Mode helper",
    namespace: "gkl-sf",
    author: "gkl_sf",
    description: "Use a switch to control Cooper Aspire RF9540 Protection Mode",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section {
		input "protectionSwitch", "capability.switch", title: "Select the virtual switch", required: true
		input "dimmer", "capability.switchLevel", title: "Select the RF9540 dimmer", required: true
        input "protectionEnabled", "enum", title: "Choose the Protection Enabled mode", options: ["Sequence Control","Remote Only"], multiple: false, required: true        
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
	subscribe(dimmer, "protection", dimmerProtectionChanged)
    subscribe(protectionSwitch, "switch", switchHandler)
    
    if (settings.protectionEnabled == "Sequence Control") state.protectionEnabled = "sequence"
    else state.protectionEnabled = "remote"
}

def dimmerProtectionChanged(evt) {
    def switchState = protectionSwitch.currentValue("switch") 
    log.debug "dimmer reports Protection Mode changed to ${evt.value}, current virtual switch state is ${switchState}"
    
    if ((evt.value == "disabled") && (switchState != "off")) protectionSwitch.off()
    else if ((evt.value ==  state.protectionEnabled) && (switchState != "on")) protectionSwitch.on()   
}

def switchHandler(evt) {
    def protectionMode = dimmer.currentValue("protection")
    log.debug "virtual switch flipped to ${evt.value}, current dimmer Protection Mode is ${protectionMode}"
    
    if ((evt.value == "on") && (protectionMode != state.protectionEnabled)) {
       if (state.protectionEnabled == "sequence") dimmer.protectionSequenceControl()
       else if (state.protectionEnabled == "remote") dimmer.protectionRemoteOnly()
    } 
    else if ((evt.value == "off") && (protectionMode != "disabled")) dimmer.protectionDisabled() 
}
