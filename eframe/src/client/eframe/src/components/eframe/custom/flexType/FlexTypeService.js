import ServiceUtils from '@/eframe-lib/domain/ServiceUtils'

export default {
  buildLabel(record, includeType = false) {
    return ServiceUtils.buildLabel(record.flexType, includeType ? 'label.flexType' : undefined)
  },
  find(uuid, successFunction, errorFunction) {
    return ServiceUtils.find('/flexType', uuid, successFunction, errorFunction)
  },
  list(options, successFunction, errorFunction) {
    return ServiceUtils.list('/flexType', options, successFunction, errorFunction)
  },
  delete(object, successFunction, errorFunction) {
    return ServiceUtils.delete('/flexType', object, successFunction, errorFunction)
  },
  save(object, fields, successFunction, errorFunction) {
    return ServiceUtils.save('/flexType', object, fields, successFunction, errorFunction)
  },
}
