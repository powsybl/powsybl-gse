def l = network.getLine('${lineId}')
[
   r: l.r,
   x: l.x,
   g1: l.g1,
   g2: l.g2,
   b1: l.b1,
   b2: l.b2,
   idVoltageLevel1: l.terminal1.voltageLevel.id,
   idVoltageLevel2: l.terminal2.voltageLevel.id,
   nameVoltageLevel1: l.terminal1.voltageLevel.name,
   nameVoltageLevel2: l.terminal2.voltageLevel.name
]
