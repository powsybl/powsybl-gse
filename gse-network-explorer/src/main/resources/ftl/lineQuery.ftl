def l = network.getLine('${lineId}')
[
   r: l.r,
   x: l.x,
   g1: l.g1,
   g2: l.g2,
   b1: l.b1,
   b2: l.b2,
   voltageLevel1: l.terminal1.voltageLevel.id,
   voltageLevel2: l.terminal2.voltageLevel.id
]
