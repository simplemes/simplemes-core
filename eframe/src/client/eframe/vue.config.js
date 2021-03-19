//const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const CompressionPlugin = require('compression-webpack-plugin');
//const path = require('path');

module.exports = {
  publicPath: process.env.NODE_ENV === 'production' ? '/client/eframe' : '/',
  pages: {
    index: {
      // entry for the page
      entry: 'src/entry/index.js',
      // the source template
      template: 'public/index.html',
      // output as dist/index.html
      filename: 'index.html',
      // when using title option,
      // template title tag needs to be <title><%= htmlWebpackPlugin.options.title %></title>
      title: 'Index Page',
      // chunks to include on this page, by default includes
      // extracted common chunks and vendor chunks.
      chunks: ['chunk-vendors', 'chunk-common', 'index']
    },
    'flexType': {
      entry: 'src/entry/custom/flexType/flexType.js',
      title: 'FlexType',
    },
  },
  devServer: {
    proxy: 'http://localhost:8080'
  },
  configureWebpack: {
    plugins: [
      //new BundleAnalyzerPlugin()  // https://www.npmjs.com/package/webpack-bundle-analyzer
      new CompressionPlugin({threshold: 500})
    ],
  },
}