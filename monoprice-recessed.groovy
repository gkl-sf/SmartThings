/**
 *  Monoprice Recessed Sensor (Vision ZD2105US-5)
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
 *  Version History
 *
 *  0.9 7/23/16
 *  1.0 7/25/16 bug fixes
 *
 */
metadata {
	definition (name: "Monoprice Recessed Sensor", namespace: "gkl-sf", author: "gkl_sf") {
		capability "Battery"
		capability "Contact Sensor"
		capability "Sensor"
		capability "Tamper Alert"
		capability "Configuration"
        
        fingerprint type: "0701", mfr: "0109", prod: "2022", model: "2201", cc:"5E,98", sec:"86,72,5A,85,59,73,80,71,84,7A,20"        
    /*
    
    Command Classes:
    
    0x85: Association V2
    0x59: Association Grp Info V2
    0x80: Battery
    0x5A: Device Reset Locally
    0x7A: Firmware Update Md V2
    0x72: Manufacturer Specific V2          
    0x71: Notification V4 (ST V3)
    0x73: Powerlevel
    0x98: Security    
    0x86: Version V2 (ST V1)
    0x84: Wake Up V2
    0x5E: Z Wave Plus Info
    
    */
         
	}

	simulator {
	}

	tiles(scale: 2) {
    
        multiAttributeTile(name:"contact", type:"generic", width:6, height:4, canChangeIcon: true) {
            tileAttribute("device.contact", key: "PRIMARY_CONTROL") {
                attributeState "open", label: '${name}', icon:"st.contact.contact.open", backgroundColor:"#ffa81e"
                attributeState "closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#ffffff"
            }
        }

		valueTile("battery", "device.battery", decoration: "flat", width: 2, height: 2) {
			state "battery", label:'${currentValue}% battery', unit:"%"
		}
        
		valueTile("tamper", "device.tamper", decoration: "flat", width: 2, height: 2) {
			state "tamper", label:'Tamper:\n${currentValue}'
		}           
        
		main "contact"
		details(["contact", "battery", "tamper"])        
	}
}

def parse(String description) {
	def result = []
	if (description.startsWith("Err 106")) {
		if (state.sec) {
			log.debug "Err 106"
		} 
        else {
			result = createEvent(name: "secureInclusion", value: "failed", descriptionText: "Failed to complete security key exchange", isStateChange: true)
		}
	} 
    else {
		def cmd = zwave.parse(description, [0x80: 1, 0x84: 2, 0x71: 3, 0x72: 2])
		if (cmd) {
			result = zwaveEvent(cmd)
		}
	}
    log.debug "--- description: ${description}"
    if (cmd) log.debug "--- parsed: ${cmd}"
    if (result) log.debug "--- result: ${result}"
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand([0x80: 1, 0x84: 2, 0x71: 3, 0x72: 2])
	log.debug "--- encapsulated: $encapsulatedCommand"
	if (encapsulatedCommand) {
		state.sec = true
		zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd) {
    if (cmd.notificationType == 0x06) { //contact event
        if (cmd.event == 0x16) {
            return createEvent(name: "contact", value: "open", descriptionText: "Door is open", isStateChange: true)
        }
        else if (cmd.event == 0x17) {
            return createEvent(name: "contact", value: "closed", descriptionText: "Door is closed", isStateChange: true)        
        }
        else {
            log.debug "Unknown contact event: $cmd.event"
        }
    }
    else if (cmd.notificationType == 0x07) { //tamper event
        if (cmd.event == 0x03) {
            return createEvent(name: "tamper", value: "detected", descriptionText: "Tamper detected", isStateChange: true)
        }
        else if (cmd.event == 0x00) {
            return createEvent(name: "tamper", value: "clear", descriptionText: "Tamper cleared", isStateChange: true)        
        }
        else {
            log.debug "Unknown tamper event: $cmd.event"
        }
    }    
        
    else {
        log.debug "Unknown notificationType: $cmd.notificationType event: $cmd.event"
    }
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd) {
	def evt = createEvent(descriptionText: "Woke up", displayed: false)
	def cmds = []
	if (!state.lastbat || now() - state.lastbat > 48*60*60*1000) { //battery report every 48hrs
		cmds << zwave.batteryV1.batteryGet()
	}
    else {
		cmds << zwave.wakeUpV2.wakeUpNoMoreInformation()
	}
	return [evt, response(delayBetween(commands(cmds)))]
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
    def evt
    if (cmd.batteryLevel == 0xff) {
        evt = createEvent(name: "battery", value: 1, unit: "%", descriptionText: "Low battery level", isStateChange: true)
    }
    else {
        state.lastbat = now()
        evt = createEvent(name: "battery", value: cmd.batteryLevel, unit: "%", descriptionText: "Battery level ${cmd.batteryLevel}%", isStateChange: true)
    }
    return [evt, response(delayBetween(commands([zwave.wakeUpV2.wakeUpNoMoreInformation()])))]
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpIntervalReport cmd) {
    def wakeUpHours = Math.round(cmd.seconds.toInteger() / 3600)
    return createEvent(name: "wakeUpInterval", value: cmd.seconds, descriptionText: "WakeUp interval is ${wakeUpHours} hours", isStateChange: true)
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.debug "Unparsed cmd: ${cmd}"
    return []
}

def configure() {
	commands([zwave.batteryV1.batteryGet(), zwave.wakeUpV2.wakeUpIntervalGet()])
}

def updated() {
    response(delayBetween(configure()))
}

private command(physicalgraph.zwave.Command cmd) {
	if (state.sec) return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    else return cmd.format()
}

private commands(commands, delay=200) {
	delayBetween(commands.collect{ command(it) }, delay)
}
