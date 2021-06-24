import ServiceUtils from '@/eframe-lib/domain/ServiceUtils'

export default {
  buildLabel(record, includeType = false) {
    return ServiceUtils.buildLabel(record.name, includeType ? 'label.allFieldsDomain' : undefined)
  },
  find(uuid, successFunction, errorFunction) {
    return ServiceUtils.find('/allFieldsDomain', uuid, successFunction, errorFunction)
  },
  list(options, successFunction, errorFunction) {
    return ServiceUtils.list('/allFieldsDomain', options, successFunction, errorFunction)
  },
  delete(object, successFunction, errorFunction) {
    return ServiceUtils.delete('/allFieldsDomain', object, successFunction, errorFunction)
  },
  save(object, fields, successFunction, errorFunction) {
    return ServiceUtils.save('/allFieldsDomain', object, fields, successFunction, errorFunction)
  },
}
