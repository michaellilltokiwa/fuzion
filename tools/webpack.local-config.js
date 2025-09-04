// webpack.local-config.js
module.exports = (webpackConfig, serverConfig) => {
  serverConfig.host = '0.0.0.0';
};
