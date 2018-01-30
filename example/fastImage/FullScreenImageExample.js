import * as React from 'react'
import { Dimensions, View, StyleSheet } from 'react-native'
import FastImage from 'react-native-fast-image'

const { width, height } = Dimensions.get('screen')
const uri = 'http://interfacelift.com/wallpaper/D4972e59/04149_mountainlake_2880x1800.jpg'
const otherUri = "https://cdn-images-1.medium.com/max/2000/1*0GFxm7MwvBLAWgh9Cma_Dw@2x.png"

export default class FullScreenExample extends React.Component {
  render() {
    return <View style={styles.container}>
      <FastImage source={{ uri: otherUri }} style={styles.image} resizeMode={FastImage.resizeMode.cover} />
    </View>
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  image: {
    width,
    height,
  }
})