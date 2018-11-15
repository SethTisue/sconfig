package com.typesafe.config.impl;

import java.util.Collection;

final class ConfigNodeArray extends ConfigNodeComplexValue {
    ConfigNodeArray(Collection<AbstractConfigNode> children) {
        super(children);
    }

    @Override
    public ConfigNodeArray newNode(Collection<AbstractConfigNode> nodes) {
        return new ConfigNodeArray(nodes);
    }
}
