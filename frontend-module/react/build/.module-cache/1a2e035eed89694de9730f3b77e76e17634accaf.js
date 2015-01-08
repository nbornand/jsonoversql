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
      React.DOM.ul({onClick: this.handleClick, style: this.style}, 
         libraries.map(function(l){
            return React.DOM.li(null, l.name, " ", React.DOM.a({href: l.url}, l.url))
        }) 
      )
    );
  }
});


React.renderComponent(
  LikeButton(null),
  document.getElementById('example')
);