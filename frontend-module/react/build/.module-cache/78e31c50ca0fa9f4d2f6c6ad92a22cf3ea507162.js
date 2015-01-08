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
	var divStyle = {height: 10};
    return (
      React.DOM.p({onClick: this.handleClick, style: this.style}, 
        "You ", text, " this. Click to toggle." 
      )
    );
  }
});

React.renderComponent(
  LikeButton(null),
  document.getElementById('example')
);