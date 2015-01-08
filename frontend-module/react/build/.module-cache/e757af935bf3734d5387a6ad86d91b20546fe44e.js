/** @jsx React.DOM */
var LikeButton = React.createClass({displayName: 'LikeButton',
  getInitialState: function() {
    return {items:[1,2,3,4]};
  },
  handleClick: function(event) {
    this.state.items.push(this.state.items.length);
      console.log('click')
    //this.setState({items: this.state.items});
  },
  render: function() {
    var text = this.state.liked ? 'like' : 'unlike';
    return (
      /*<ul style={this.style}>
        { this.state.items.map(function(l){
            console.log('redraw');
            return <li key={l}>link{l}</li>
        }) }
      </ul>*/
      React.DOM.p({onMouseOver: this.handleClick})
    );
  }
});

var Cmp = React.createClass({displayName: 'Cmp',
  render: function() {
    return React.DOM.p(null, 'text'); 
  }
}); 

React.renderComponent(
  LikeButton(null),
  document.getElementById('example')
);