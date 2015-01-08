/** @jsx React.DOM */
var LikeButton = React.createClass({displayName: 'LikeButton',
  getInitialState: function() {
    return {liked: false};
  },
  handleClick: function(event) {
    this.setState({liked: !this.state.liked});
  },
  render: function() {
    var text = this.state.liked ? 'like' : 'unlike';
    return (
      React.DOM.p({onClick: this.handleClick}, 
        "You ", text, " this. Click to toggle." 
      )
    );
  }
});

React.renderComponent(
  LikeButton(null),
  document.getElementById('example')
);