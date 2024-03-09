package mcjty.xnet.utils;

public interface ChoiceEvent<T> {
    void choiceChanged(T newChoice);
}
