import React from 'react'
import { TabNavigator, TabBarBottom } from 'react-navigation'
import FastImageExample from './fastImage/FastImageExample'
import FastImageGrid from './fastImage/FastImageGrid'
import DefaultImageGrid from './fastImage/DefaultImageGrid'
import FullScreenImageExample from './fastImage/FullScreenImageExample'

const App = TabNavigator(
  {
    fastImageExample: {
      screen: FastImageExample,
    },
    image: {
      screen: DefaultImageGrid,
    },
    fastImage: {
      screen: FastImageGrid,
    },
    fullScreen: {
      screen: FullScreenImageExample,
    }
  },
  {
    tabBarComponent: TabBarBottom,
    tabBarPosition: 'bottom',
    swipeEnabled: false,
    animationEnabled: false,
    tabBarOptions: {
      style: {
        backgroundColor: 'white',
      },
    },
  },
)

export default App
