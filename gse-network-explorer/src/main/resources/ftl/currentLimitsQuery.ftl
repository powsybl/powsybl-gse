def queryLimits(limits, side) {
    result = [ side: side ]
    if (limits) {
        if (limits.permanentLimit) {
            result['permanentLimit'] = limits.permanentLimit
        }
        result['temporaryLimits'] = []
        for (tl in limits.temporaryLimits) {
            result['temporaryLimits'] << [ name: tl.name,
                                           value: tl.value,
                                           acceptableDuration: tl.acceptableDuration ]
        }
    }
    result
}

def l = network.getLine('${lineId}')

[ queryLimits(l.currentLimits1, 'Side 1'),
  queryLimits(l.currentLimits2, 'Side 2') ]
