import ServiceUtils from '@/eframe-lib/domain/ServiceUtils'

export default {
  buildLabel(record, includeType = false) {
    return ServiceUtils.buildLabel(record.flexType, includeType ? 'label.sampleParent' : undefined)
  },
  find(uuid, successFunction, errorFunction) {
    return ServiceUtils.find('/sampleParent', uuid, successFunction, errorFunction)
  },
  list(options, successFunction, errorFunction) {
    return ServiceUtils.list('/sampleParent', options, successFunction, errorFunction)
  },
  delete(object, successFunction, errorFunction) {
    return ServiceUtils.delete('/sampleParent', object, successFunction, errorFunction)
  },
  save(object, successFunction, errorFunction) {
    return ServiceUtils.save('/sampleParent', object, successFunction, errorFunction)
  },
}
