def s = network.getSubstation('${substationId}')
s.voltageLevels.collect {
    [
        idAndName: [
                       id: it.id,
                       name: it.name
                   ],
        equipments: it.connectables.collect {
                        [
                            type: it.type.name() == 'SHUNT_COMPENSATOR' ? (it.getbPerSection() > 0 ? 'CAPACITOR' : 'INDUCTOR')
                                                                        : it.type,
                            idAndName: [
                                           id: it.id,
                                           name: it.name
                                       ]
                        ]
                    }
                    +
                    it.switches.collect {
                        [
                            type: 'SWITCH',
                            idAndName: [
                                           id: it.id,
                                           name: it.name
                                       ]
                        ]
                    }
    ]
}
