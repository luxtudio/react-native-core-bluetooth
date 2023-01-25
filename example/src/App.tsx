import React from 'react';

import { StyleSheet, View, Text } from 'react-native';
import {} from '@luxtudio/core-bluetooth';

export default function App() {
  return (
    <View style={styles.container}>
      <Text>Core Bluetooth</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
