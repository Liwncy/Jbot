// noinspection JSUnresolvedReference,JSUnusedGlobalSymbols

const map = {}
const flagMap = {}

const Listener = Java.extend(EventListenerClass, {
    support(e, s){
        return s.isGroup()
    },
    onEvent(e, s) {
        const group = s.getSender();
        const content = s.getContent();
        if (map[group.getId()] === content && flagMap[group.getId()] !== content) {
            flagMap[group.getId()] = content
            group.send(content);
            return true
        }
        map[group.getId()] = content
        return false
    },
    executeNext() {
        return true;
    },
    getEventClass() {
        return MessageEventClass
    },
    getSourceClass() {
        return TextMessageClass
    }
})

export {Listener}